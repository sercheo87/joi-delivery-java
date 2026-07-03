package com.tw.joi.delivery.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tw.joi.delivery.domain.Order;
import com.tw.joi.delivery.domain.OrderStatus;
import com.tw.joi.delivery.seedData.SeedData;
import java.math.BigDecimal;
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
class FeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SeedData.orders.clear();
        SeedData.feedbacks.clear();
        SeedData.notifications.clear();
    }

    private Order createDeliveredOrder(String orderId, String userId, String outletId) {
        Order order = Order.builder()
            .orderId(orderId)
            .userId(userId)
            .outletId(outletId)
            .products(new ArrayList<>())
            .status(OrderStatus.DELIVERED)
            .totalAmount(BigDecimal.valueOf(50.0))
            .placedAt(LocalDateTime.now().minusHours(2))
            .estimatedDeliveryTime(LocalDateTime.now().minusHours(1))
            .build();
        SeedData.orders.add(order);
        return order;
    }

    private Order createOrderWithStatus(String orderId, String userId, OrderStatus status) {
        Order order = Order.builder()
            .orderId(orderId)
            .userId(userId)
            .outletId("store101")
            .products(new ArrayList<>())
            .status(status)
            .totalAmount(BigDecimal.valueOf(50.0))
            .placedAt(LocalDateTime.now().minusHours(2))
            .estimatedDeliveryTime(LocalDateTime.now().minusHours(1))
            .build();
        SeedData.orders.add(order);
        return order;
    }

    // ── POST /feedback ───────────────────────────────────────────────────────

    @Test
    void shouldSubmitFeedbackOnDeliveredOrder() throws Exception {
        createDeliveredOrder("order-delivered", "user101", "store101");

        mockMvc.perform(MockMvcRequestBuilders.post("/feedback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":\"user101\",\"orderId\":\"order-delivered\",\"type\":\"ORDER\",\"rating\":5,\"comment\":\"Great service!\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.feedbackId").isNotEmpty())
            .andExpect(jsonPath("$.userId").value("user101"))
            .andExpect(jsonPath("$.orderId").value("order-delivered"))
            .andExpect(jsonPath("$.type").value("ORDER"))
            .andExpect(jsonPath("$.rating").value(5))
            .andExpect(jsonPath("$.comment").value("Great service!"))
            .andExpect(jsonPath("$.submittedAt").isNotEmpty());
    }

    @Test
    void shouldSubmitAppFeedbackWithNoOrderId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/feedback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":\"user101\",\"type\":\"APP\",\"rating\":4,\"comment\":\"Love the app!\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.feedbackId").isNotEmpty())
            .andExpect(jsonPath("$.userId").value("user101"))
            .andExpect(jsonPath("$.type").value("APP"))
            .andExpect(jsonPath("$.rating").value(4));
    }

    @Test
    void shouldReturn400WhenRatingIsInvalid() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/feedback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":\"user101\",\"type\":\"APP\",\"rating\":0,\"comment\":\"Bad\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenRatingExceedsMax() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/feedback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":\"user101\",\"type\":\"APP\",\"rating\":6,\"comment\":\"Too high\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenUserNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/feedback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":\"user-nonexistent\",\"type\":\"APP\",\"rating\":3,\"comment\":\"Hmm\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenOrderNotFoundOnSubmit() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/feedback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":\"user101\",\"orderId\":\"order-nonexistent\",\"type\":\"ORDER\",\"rating\":5,\"comment\":\"Good\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenOrderBelongsToAnotherUser() throws Exception {
        // Order belongs to "user-other"; user101 (who exists) tries to rate it → 403
        createDeliveredOrder("order-delivered", "user-other", "store101");

        mockMvc.perform(MockMvcRequestBuilders.post("/feedback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":\"user101\",\"orderId\":\"order-delivered\",\"type\":\"ORDER\",\"rating\":5,\"comment\":\"Good\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn400WhenOrderIsNotDelivered() throws Exception {
        createOrderWithStatus("order-preparing", "user101", OrderStatus.PREPARING);

        mockMvc.perform(MockMvcRequestBuilders.post("/feedback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":\"user101\",\"orderId\":\"order-preparing\",\"type\":\"ORDER\",\"rating\":5,\"comment\":\"Early\"}"))
            .andExpect(status().isBadRequest());
    }

    // ── GET /feedback/user?userId=... ────────────────────────────────────────

    @Test
    void shouldGetFeedbackByUser() throws Exception {
        createDeliveredOrder("order-d1", "user101", "store101");

        mockMvc.perform(MockMvcRequestBuilders.post("/feedback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":\"user101\",\"orderId\":\"order-d1\",\"type\":\"ORDER\",\"rating\":5,\"comment\":\"Good\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.get("/feedback/user")
                            .param("userId", "user101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].userId").value("user101"));
    }

    @Test
    void shouldReturnEmptyListWhenNoFeedbackForUser() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/feedback/user")
                            .param("userId", "user101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    // ── GET /feedback/order/{orderId} ────────────────────────────────────────

    @Test
    void shouldGetFeedbackByOrder() throws Exception {
        createDeliveredOrder("order-d1", "user101", "store101");

        mockMvc.perform(MockMvcRequestBuilders.post("/feedback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":\"user101\",\"orderId\":\"order-d1\",\"type\":\"ORDER\",\"rating\":4,\"comment\":\"Nice\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.get("/feedback/order/{orderId}", "order-d1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].orderId").value("order-d1"));
    }

    @Test
    void shouldReturn404WhenOrderNotFoundForGetByOrder() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/feedback/order/{orderId}", "order-nonexistent"))
            .andExpect(status().isNotFound());
    }

    // ── GET /feedback/store/{storeId}/rating ─────────────────────────────────

    @Test
    void shouldGetAverageRatingForStore() throws Exception {
        createDeliveredOrder("order-s1", "user101", "store101");
        createDeliveredOrder("order-s2", "user101", "store101");

        mockMvc.perform(MockMvcRequestBuilders.post("/feedback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":\"user101\",\"orderId\":\"order-s1\",\"type\":\"ORDER\",\"rating\":4,\"comment\":\"Good\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.post("/feedback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":\"user101\",\"orderId\":\"order-s2\",\"type\":\"ORDER\",\"rating\":5,\"comment\":\"Great\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.get("/feedback/store/{storeId}/rating", "store101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.storeId").value("store101"))
            .andExpect(jsonPath("$.averageRating").value(4.5))
            .andExpect(jsonPath("$.totalReviews").value(2));
    }

    @Test
    void shouldReturnZeroAverageWhenNoReviewsForStore() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/feedback/store/{storeId}/rating", "store101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.storeId").value("store101"))
            .andExpect(jsonPath("$.averageRating").value(0.0))
            .andExpect(jsonPath("$.totalReviews").value(0));
    }
}
