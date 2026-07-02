package com.tw.joi.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.tw.joi.delivery.domain.GroceryProduct;
import com.tw.joi.delivery.dto.response.ProductResponse;
import com.tw.joi.delivery.dto.response.ProductSearchResponse;
import com.tw.joi.delivery.seedData.SeedData;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private StoreService storeService;

    @InjectMocks
    private ProductService productService;

    @Test
    void shouldReturnProductsForExistingStore() {
        when(storeService.findById("store101")).thenReturn(Optional.of(SeedData.store101));

        List<ProductResponse> result = productService.getProductsByStore("store101");

        assertThat(result).hasSize(3);
        assertThat(result).extracting(ProductResponse::productId)
            .containsExactlyInAnyOrder("product101", "product102", "product103");
    }

    @Test
    void shouldReturnProductsForStore102() {
        when(storeService.findById("store102")).thenReturn(Optional.of(SeedData.store102));

        List<ProductResponse> result = productService.getProductsByStore("store102");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ProductResponse::productId)
            .containsExactlyInAnyOrder("product104", "product105");
    }

    @Test
    void shouldThrow404WhenStoreNotFoundForGetProductsByStore() {
        when(storeService.findById("unknownStore")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductsByStore("unknownStore"))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldReturnProductsMatchingQueryCaseInsensitively() {
        List<ProductSearchResponse> result = productService.searchProducts("bread");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).productName()).isEqualToIgnoringCase("Wheat Bread");
        assertThat(result.get(0).storeId()).isEqualTo("store101");
    }

    @Test
    void shouldReturnProductsMatchingQueryUpperCase() {
        List<ProductSearchResponse> result = productService.searchProducts("BREAD");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).productId()).isEqualTo("product101");
    }

    @Test
    void shouldReturnEmptyListWhenNoProductsMatchSearch() {
        List<ProductSearchResponse> result = productService.searchProducts("nonexistentproduct");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnProductsFromAllStoresWhenSearching() {
        List<ProductSearchResponse> result = productService.searchProducts("rice");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).productId()).isEqualTo("product105");
        assertThat(result.get(0).storeId()).isEqualTo("store102");
    }

    @Test
    void shouldReturnProductDetailForExistingProduct() {
        when(storeService.findById("store101")).thenReturn(Optional.of(SeedData.store101));

        GroceryProduct product = productService.getProductDetail("product101", "store101");

        assertThat(product).isNotNull();
        assertThat(product.getProductId()).isEqualTo("product101");
        assertThat(product.getProductName()).isEqualTo("Wheat Bread");
    }

    @Test
    void shouldThrow404WhenProductNotFound() {
        when(storeService.findById("store101")).thenReturn(Optional.of(SeedData.store101));

        assertThatThrownBy(() -> productService.getProductDetail("unknownProduct", "store101"))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldThrow404WhenStoreNotFoundForGetProductDetail() {
        when(storeService.findById("unknownStore")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductDetail("product101", "unknownStore"))
            .isInstanceOf(ResponseStatusException.class);
    }
}
