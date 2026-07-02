package com.tw.joi.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tw.joi.delivery.domain.GroceryProduct;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductServiceTest {

    private final ProductService productService = new ProductService();

    @Test
    @DisplayName("Given an existing product and outlet, when the product is requested, then the matching product is returned")
    void shouldGetProductByProductIdAndOutletId() {
        // Given
        String productId = "product101";
        String outletId = "store101";

        // When
        GroceryProduct product = productService.getProduct(productId, outletId);

        // Then
        assertThat(product).isNotNull();
        assertThat(product.getProductId()).isEqualTo(productId);
        assertThat(product.getProductName()).isEqualTo("Wheat Bread");
        assertThat(product.getStore().getOutletId()).isEqualTo(outletId);
    }

    @Test
    @DisplayName("Given an unknown product, when the product is requested, then no product is returned")
    void shouldReturnNullWhenProductDoesNotExist() {
        // Given
        String productId = "unknown-product";
        String outletId = "store101";

        // When
        GroceryProduct product = productService.getProduct(productId, outletId);

        // Then
        assertThat(product).isNull();
    }

    @Test
    @DisplayName("Given a product in another outlet, when the product is requested for the wrong outlet, then no product is returned")
    void shouldReturnNullWhenOutletDoesNotMatch() {
        // Given
        String productId = "product101";
        String outletId = "store102";

        // When
        GroceryProduct product = productService.getProduct(productId, outletId);

        // Then
        assertThat(product).isNull();
    }
}
