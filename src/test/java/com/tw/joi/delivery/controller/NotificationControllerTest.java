package com.tw.joi.delivery.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tw.joi.delivery.domain.Cart;
import com.tw.joi.delivery.domain.Notification;
import com.tw.joi.delivery.domain.Order;
import com.tw.joi.delivery.seedData.SeedData;
import java.time.LocalDateTime;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SeedData.orders.clear();
        SeedData.trackingEvents.clear();
        SeedData.notifications.clear();
        Cart cart = SeedData.cartForUsers.get("user101");
        cart.setProducts(new ArrayList<>(SeedData.groceryProducts.subList(0, 1)));
    }

    // GET /notifications?userId=user101

    @Test
    void shouldReturnEmptyNotificationsWhenNoneExist() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/notifications")
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldReturnNotificationsAfterPlacingOrder() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/orders/place")
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.get("/notifications")
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].title").value("Order Confirmed"))
            .andExpect(jsonPath("$[0].userId").value("user101"))
            .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    void shouldReturnNotificationsInMostRecentFirstOrder() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/orders/place")
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated());

        Order order = SeedData.orders.get(0);

        // Reset cart to place another call - we'll just update status
        mockMvc.perform(MockMvcRequestBuilders.patch("/orders/{orderId}/status", order.getOrderId())
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"PREPARING\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.get("/notifications")
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].title").value("Order Being Prepared"));
    }

    // GET /notifications/unread-count?userId=user101

    @Test
    void shouldReturnZeroUnreadCountWhenNoNotifications() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/notifications/unread-count")
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unreadCount").value(0));
    }

    @Test
    void shouldReturnCorrectUnreadCountAfterPlacingOrder() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/orders/place")
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.get("/notifications/unread-count")
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unreadCount").value(1));
    }

    // PATCH /notifications/{notificationId}/read?userId=user101

    @Test
    void shouldMarkNotificationAsRead() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/orders/place")
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated());

        Notification notification = SeedData.notifications.get(0);

        mockMvc.perform(MockMvcRequestBuilders.patch("/notifications/{notificationId}/read",
                                                     notification.getNotificationId())
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notificationId").value(notification.getNotificationId()))
            .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    void shouldReturn404WhenMarkingNonExistentNotificationAsRead() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch("/notifications/{notificationId}/read",
                                                     "nonexistent-id")
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenMarkingNotificationBelongingToAnotherUser() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/orders/place")
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated());

        Notification notification = SeedData.notifications.get(0);

        mockMvc.perform(MockMvcRequestBuilders.patch("/notifications/{notificationId}/read",
                                                     notification.getNotificationId())
                            .param("userId", "user999")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    // PATCH /notifications/read-all?userId=user101

    @Test
    void shouldMarkAllNotificationsAsRead() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/orders/place")
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated());

        Order order = SeedData.orders.get(0);

        mockMvc.perform(MockMvcRequestBuilders.patch("/orders/{orderId}/status", order.getOrderId())
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"PREPARING\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.patch("/notifications/read-all")
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].read").value(true))
            .andExpect(jsonPath("$[1].read").value(true));
    }

    @Test
    void shouldReturnEmptyListWhenMarkAllAsReadForUserWithNoNotifications() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch("/notifications/read-all")
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldReduceUnreadCountToZeroAfterMarkAllAsRead() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/orders/place")
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.patch("/notifications/read-all")
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.get("/notifications/unread-count")
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unreadCount").value(0));
    }

}
