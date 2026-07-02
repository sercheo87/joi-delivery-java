package com.tw.joi.delivery.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Given a store id, when inventory health is requested, then the endpoint responds successfully")
    void shouldReturnTheHealthOfTheStore() throws Exception {
        // Given
        String getUrl = "/inventory/health?storeId={storeId}";

        // When / Then
        mockMvc.perform(get(getUrl, "store101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }
}
