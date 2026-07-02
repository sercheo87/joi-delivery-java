package com.tw.joi.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tw.joi.delivery.domain.Cart;
import com.tw.joi.delivery.dto.request.AddProductRequest;
import com.tw.joi.delivery.dto.response.CartProductInfo;
import com.tw.joi.delivery.seedData.SeedData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CartServiceTest {

    private static final String USER_ID = "user101";
    private static final String PRODUCT_ID = "product101";
    private static final String OUTLET_ID = "store101";

    private CartService cartService;

    @BeforeEach
    void setUp() {
        SeedData.cartForUsers.get(USER_ID).getProducts().clear();
        cartService = new CartService(new UserService(), new ProductService());
    }

    @Test
    @DisplayName("Given an existing user and product, when the product is added, then the service returns cart and product information")
    void shouldAddProductToCartForUser() {
        // Given
        AddProductRequest request = new AddProductRequest();
        request.setUserId(USER_ID);
        request.setProductId(PRODUCT_ID);
        request.setOutletId(OUTLET_ID);

        // When
        CartProductInfo cartProductInfo = cartService.addProductToCartForUser(request);

        // Then
        assertThat(cartProductInfo.cart().getCartId()).isEqualTo("cart101");
        assertThat(cartProductInfo.cart().getProducts()).hasSize(1);
        assertThat(cartProductInfo.product().getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(cartProductInfo.sellingPrice()).isNull();
    }

    @Test
    @DisplayName("Given an existing user, when their cart is requested, then the user's cart is returned")
    void shouldGetCartForUser() {
        // Given
        String userId = USER_ID;

        // When
        Cart cart = cartService.getCartForUser(userId);

        // Then
        assertThat(cart.getCartId()).isEqualTo("cart101");
        assertThat(cart.getUser().getUserId()).isEqualTo(USER_ID);
        assertThat(cart.getProducts()).isEmpty();
    }
}
