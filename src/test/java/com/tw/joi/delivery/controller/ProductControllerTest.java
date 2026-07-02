package com.tw.joi.delivery.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tw.joi.delivery.domain.GroceryProduct;
import com.tw.joi.delivery.domain.GroceryStore;
import com.tw.joi.delivery.dto.response.ProductResponse;
import com.tw.joi.delivery.dto.response.ProductSearchResponse;
import com.tw.joi.delivery.service.ProductService;
import java.math.BigDecimal;
import java.util.List;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(ProductController.class)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void shouldReturnProductsForStore() throws Exception {
        String url = "/products?storeId={storeId}";
        List<ProductResponse> products = List.of(
            new ProductResponse("product101", "Wheat Bread", BigDecimal.valueOf(10.5), 30)
        );
        when(productService.getProductsByStore("store101")).thenReturn(products);

        mockMvc.perform(MockMvcRequestBuilders.get(url, "store101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].productId", Is.is("product101")))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].productName", Is.is("Wheat Bread")))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].availableStock", Is.is(30)));
    }

    @Test
    void shouldReturn404WhenStoreNotFoundForProductList() throws Exception {
        String url = "/products?storeId={storeId}";
        when(productService.getProductsByStore("unknownStore"))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));

        mockMvc.perform(MockMvcRequestBuilders.get(url, "unknownStore")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnProductsMatchingSearchQuery() throws Exception {
        String url = "/products/search?query={query}";
        List<ProductSearchResponse> results = List.of(
            new ProductSearchResponse("product101", "Wheat Bread", BigDecimal.valueOf(10.5), 30, "store101")
        );
        when(productService.searchProducts("bread")).thenReturn(results);

        mockMvc.perform(MockMvcRequestBuilders.get(url, "bread")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].productId", Is.is("product101")))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].productName", Is.is("Wheat Bread")))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].storeId", Is.is("store101")));
    }

    @Test
    void shouldReturnProductById() throws Exception {
        String url = "/products/{productId}?storeId={storeId}";
        GroceryStore store = GroceryStore.builder().name("Fresh Picks").outletId("store101").build();
        GroceryProduct product = GroceryProduct.builder()
            .productId("product101")
            .productName("Wheat Bread")
            .mrp(BigDecimal.valueOf(10.5))
            .availableStock(30)
            .store(store)
            .build();
        when(productService.getProductDetail("product101", "store101")).thenReturn(product);

        mockMvc.perform(MockMvcRequestBuilders.get(url, "product101", "store101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.productId", Is.is("product101")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.productName", Is.is("Wheat Bread")));
    }

    @Test
    void shouldReturn404WhenProductNotFound() throws Exception {
        String url = "/products/{productId}?storeId={storeId}";
        when(productService.getProductDetail("unknownProduct", "store101"))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        mockMvc.perform(MockMvcRequestBuilders.get(url, "unknownProduct", "store101")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenStoreNotFoundForProductDetail() throws Exception {
        String url = "/products/{productId}?storeId={storeId}";
        when(productService.getProductDetail("product101", "unknownStore"))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));

        mockMvc.perform(MockMvcRequestBuilders.get(url, "product101", "unknownStore")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }
}
