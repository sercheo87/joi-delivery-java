package com.tw.joi.delivery.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tw.joi.delivery.dto.request.AddProductRequest;
import com.tw.joi.delivery.seedData.SeedData;
import org.junit.jupiter.api.BeforeEach;
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
class CartControllerTest {

    private static final String USER_ID = "user101";
    private static final String PRODUCT_ID = "product101";
    private static final String OUTLET_ID = "store101";

    private final ObjectMapper mapper = new ObjectMapper();
    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SeedData.cartForUsers.get(USER_ID).getProducts().clear();
    }

    @Test
    @DisplayName("Given an existing user and product, when the product is added to the cart, then the cart contains that product")
    void shouldAddTheRequestedProductToTheCart() throws Exception {
        // Given
        AddProductRequest addProductRequest = new AddProductRequest();
        addProductRequest.setProductId(PRODUCT_ID);
        addProductRequest.setUserId(USER_ID);
        addProductRequest.setOutletId(OUTLET_ID);

        // When / Then
        mockMvc.perform(post("/cart/product")
                            .content(mapper.writeValueAsString(addProductRequest))
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.cart.cartId", is("cart101")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.cart.products", hasSize(1)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.cart.products[0].productId", is(PRODUCT_ID)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.cart.user.userId", is(USER_ID)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.product.productId", is(PRODUCT_ID)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.sellingPrice", nullValue()));
    }

    @Test
    @DisplayName("Given an existing user with an empty cart, when the cart is viewed, then the user's cart is returned")
    void shouldReturnTheCart() throws Exception {
        // Given
        String url = "/cart/view?userId={userId}";

        // When / Then
        mockMvc.perform(get(url, USER_ID)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.cartId", is("cart101")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.user.userId", is(USER_ID)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.products", hasSize(0)));
    }
}
