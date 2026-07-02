package com.tw.joi.delivery.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Given a store id, when inventory health is requested, then the endpoint responds with store inventory details")
    void shouldReturnTheHealthOfTheStore() throws Exception {
        // Given
        String getUrl = "/inventory/health?storeId={storeId}";

        // When / Then
        mockMvc.perform(get(getUrl, "store101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.storeId", is("store101")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.storeName", is("Fresh Picks")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.overallStatus", is("HEALTHY")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.products", hasSize(3)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.products[0].productId", is("product101")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.products[0].availableStock", is(30)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.products[0].threshold", is(10)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.products[0].stockStatus", is("HEALTHY")));
    }

    @Test
    @DisplayName("Given an unknown store id, when inventory health is requested, then a 404 is returned")
    void shouldReturn404WhenStoreNotFound() throws Exception {
        // Given
        String getUrl = "/inventory/health?storeId={storeId}";

        // When / Then
        mockMvc.perform(get(getUrl, "unknown-store")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }
}
