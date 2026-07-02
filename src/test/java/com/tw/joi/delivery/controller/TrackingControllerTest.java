package com.tw.joi.delivery.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tw.joi.delivery.domain.Cart;
import com.tw.joi.delivery.domain.Order;
import com.tw.joi.delivery.domain.OrderStatus;
import com.tw.joi.delivery.domain.TrackingEvent;
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
class TrackingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private String orderId;

    @BeforeEach
    void setUp() throws Exception {
        SeedData.orders.clear();
        SeedData.trackingEvents.clear();

        Cart cart = SeedData.cartForUsers.get("user101");
        cart.setProducts(new ArrayList<>(SeedData.groceryProducts.subList(0, 1)));

        String response = mockMvc.perform(MockMvcRequestBuilders.post("/orders/place")
                                              .param("userId", "user101")
                                              .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        orderId = SeedData.orders.get(0).getOrderId();
    }

    @Test
    void shouldReturnEmptyTrackingHistoryWhenNoEvents() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/tracking/{orderId}", orderId)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldReturnTrackingHistoryAfterStatusUpdates() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch("/orders/{orderId}/status", orderId)
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"PREPARING\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.patch("/orders/{orderId}/status", orderId)
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"OUT_FOR_DELIVERY\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.get("/tracking/{orderId}", orderId)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].status").value("PREPARING"))
            .andExpect(jsonPath("$[1].status").value("OUT_FOR_DELIVERY"));
    }

    @Test
    void shouldReturn404WhenTrackingHistoryForNonExistentOrder() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/tracking/{orderId}", "order-nonexistent")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnSyntheticEventForLatestStatusWhenNoEvents() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/tracking/{orderId}/status", orderId)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value(orderId))
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void shouldReturnLatestStatusAfterStatusUpdate() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch("/orders/{orderId}/status", orderId)
                            .param("userId", "user101")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"PREPARING\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.get("/tracking/{orderId}/status", orderId)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value(orderId))
            .andExpect(jsonPath("$.status").value("PREPARING"));
    }

    @Test
    void shouldReturn404WhenLatestStatusForNonExistentOrder() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/tracking/{orderId}/status", "order-nonexistent")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }
}
