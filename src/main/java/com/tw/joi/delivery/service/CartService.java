package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.Cart;
import com.tw.joi.delivery.domain.GroceryProduct;
import com.tw.joi.delivery.domain.User;
import com.tw.joi.delivery.dto.request.AddProductRequest;
import com.tw.joi.delivery.dto.response.CartProductInfo;
import com.tw.joi.delivery.seedData.SeedData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final Map<String, Cart> userCarts = SeedData.cartForUsers;
    private final UserService userService;
    private final ProductService productService;

    public CartProductInfo addProductToCartForUser(AddProductRequest addProductRequest) {
        User user = userService.fetchUserById(addProductRequest.getUserId());
        Cart cart = fetchCartForUser(user);
        GroceryProduct product = productService.getProduct(addProductRequest.getProductId(), addProductRequest.getOutletId());
        if (product == null) {
            log.warn("Product not found: productId={} outletId={}", addProductRequest.getProductId(), addProductRequest.getOutletId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
        if (product.getAvailableStock() <= 0) {
            log.warn("Out of stock — rejected add to cart: productId={} userId={}", product.getProductId(), addProductRequest.getUserId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product is out of stock");
        }
        cart.getProducts().add(product);
        log.info("Product added to cart: productId={} userId={} cartSize={}", product.getProductId(), addProductRequest.getUserId(), cart.getProducts().size());
        return new CartProductInfo(cart, product, product.getSellingPrice());
    }

    public Cart getCartForUser(String userId) {
        User user = userService.fetchUserById(userId);
        Cart cart = fetchCartForUser(user);
        log.debug("Cart retrieved: userId={} items={}", userId, cart.getProducts().size());
        return cart;
    }

    private Cart fetchCartForUser(User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return userCarts.get(user.getUserId());
    }
}
