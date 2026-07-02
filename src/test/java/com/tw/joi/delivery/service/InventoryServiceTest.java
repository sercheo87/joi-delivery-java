package com.tw.joi.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tw.joi.delivery.dto.response.InventoryHealthResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class InventoryServiceTest {

    private final InventoryService inventoryService = new InventoryService(new StoreService(), new ProductService());

    @Test
    @DisplayName("Given a valid store id, when inventory health is requested, then health details are returned")
    void shouldReturnInventoryHealthForExistingStore() {
        // When
        InventoryHealthResponse response = inventoryService.getInventoryHealth("store101");

        // Then
        assertThat(response.storeId()).isEqualTo("store101");
        assertThat(response.storeName()).isEqualTo("Fresh Picks");
        assertThat(response.overallStatus()).isEqualTo("HEALTHY");
        assertThat(response.products()).hasSize(3);
        assertThat(response.products()).allMatch(p -> "HEALTHY".equals(p.stockStatus()));
    }

    @Test
    @DisplayName("Given an unknown store id, when inventory health is requested, then a not found exception is thrown")
    void shouldThrowNotFoundForUnknownStore() {
        assertThatThrownBy(() -> inventoryService.getInventoryHealth("unknown-store"))
            .isInstanceOf(ResponseStatusException.class);
    }
}
