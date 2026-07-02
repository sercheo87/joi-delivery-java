package com.tw.joi.delivery.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tw.joi.delivery.domain.Cart;
import com.tw.joi.delivery.domain.Order;
import com.tw.joi.delivery.domain.OrderStatus;
import com.tw.joi.delivery.seedData.SeedData;
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
class OrderControllerTest {

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

    @Test
    void shouldPlaceOrderSuccessfully() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/orders/place")
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderId").isNotEmpty())
            .andExpect(jsonPath("$.userId").value("user101"))
            .andExpect(jsonPath("$.outletId").value("store101"))
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.totalAmount").isNotEmpty());
    }

    @Test
    void shouldReturn400WhenCartIsEmpty() throws Exception {
        Cart cart = SeedData.cartForUsers.get("user101");
        cart.setProducts(new ArrayList<>());

        mockMvc.perform(MockMvcRequestBuilders.post("/orders/place")
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetOrdersByUser() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/orders/place")
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.get("/orders")
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].userId").value("user101"))
            .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    void shouldCancelOrderSuccessfully() throws Exception {
        String placeResponse = mockMvc.perform(MockMvcRequestBuilders.post("/orders/place")
                                                   .param("userId", "user101")
                                                   .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String orderId = SeedData.orders.get(0).getOrderId();

        mockMvc.perform(MockMvcRequestBuilders.delete("/orders/{orderId}", orderId)
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value(orderId))
            .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void shouldReturn400WhenCancellingNonCancellableOrder() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/orders/place")
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated());

        Order order = SeedData.orders.get(0);
        order.setStatus(OrderStatus.DELIVERED);

        mockMvc.perform(MockMvcRequestBuilders.delete("/orders/{orderId}", order.getOrderId())
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenCancellingNonExistentOrder() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/orders/{orderId}", "order-nonexistent")
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenCancellingOrderBelongingToAnotherUser() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/orders/place")
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated());

        Order order = SeedData.orders.get(0);

        mockMvc.perform(MockMvcRequestBuilders.delete("/orders/{orderId}", order.getOrderId())
                            .param("userId", "user999")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldUpdateOrderStatusSuccessfully() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/orders/place")
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated());

        Order order = SeedData.orders.get(0);

        mockMvc.perform(MockMvcRequestBuilders.patch("/orders/{orderId}/status", order.getOrderId())
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"PREPARING\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value(order.getOrderId()))
            .andExpect(jsonPath("$.status").value("PREPARING"));
    }

    @Test
    void shouldReturn400WhenInvalidStatusTransition() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/orders/place")
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated());

        Order order = SeedData.orders.get(0);

        mockMvc.perform(MockMvcRequestBuilders.patch("/orders/{orderId}/status", order.getOrderId())
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"DELIVERED\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn403WhenUpdatingOrderBelongingToAnotherUser() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/orders/place")
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated());

        Order order = SeedData.orders.get(0);

        mockMvc.perform(MockMvcRequestBuilders.patch("/orders/{orderId}/status", order.getOrderId())
                            .param("userId", "user999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"PREPARING\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentOrder() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch("/orders/{orderId}/status", "order-nonexistent")
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"PREPARING\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenUpdatingCancelledOrder() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/orders/place")
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated());

        Order order = SeedData.orders.get(0);
        order.setStatus(OrderStatus.CANCELLED);

        mockMvc.perform(MockMvcRequestBuilders.patch("/orders/{orderId}/status", order.getOrderId())
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"PREPARING\"}"))
            .andExpect(status().isBadRequest());
    }
}
