package com.tw.joi.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.tw.joi.delivery.domain.Cart;
import com.tw.joi.delivery.domain.GroceryProduct;
import com.tw.joi.delivery.domain.GroceryStore;
import com.tw.joi.delivery.domain.Order;
import com.tw.joi.delivery.domain.OrderStatus;
import com.tw.joi.delivery.seedData.SeedData;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private CartService cartService;

    @InjectMocks
    private OrderService orderService;

    private GroceryStore store;
    private GroceryProduct product;
    private Cart cart;

    @BeforeEach
    void setUp() {
        SeedData.orders.clear();

        store = GroceryStore.builder()
            .outletId("store101")
            .name("Fresh Picks")
            .build();

        product = GroceryProduct.builder()
            .productId("product101")
            .productName("Wheat Bread")
            .mrp(BigDecimal.valueOf(10.5))
            .availableStock(30)
            .store(store)
            .build();

        cart = Cart.builder()
            .cartId("cart101")
            .outlet(store)
            .products(new ArrayList<>(List.of(product)))
            .build();
    }

    @Test
    void shouldPlaceOrderSuccessfully() {
        when(cartService.getCartForUser("user101")).thenReturn(cart);

        Order order = orderService.placeOrder("user101");

        assertThat(order).isNotNull();
        assertThat(order.getOrderId()).startsWith("order-");
        assertThat(order.getUserId()).isEqualTo("user101");
        assertThat(order.getOutletId()).isEqualTo("store101");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(10.5));
        assertThat(order.getPlacedAt()).isNotNull();
        assertThat(order.getEstimatedDeliveryTime()).isEqualTo(order.getPlacedAt().plusMinutes(45));
        assertThat(order.getProducts()).hasSize(1);
    }

    @Test
    void shouldClearCartAfterPlacingOrder() {
        when(cartService.getCartForUser("user101")).thenReturn(cart);

        orderService.placeOrder("user101");

        assertThat(cart.getProducts()).isEmpty();
    }

    @Test
    void shouldAddOrderToSeedData() {
        when(cartService.getCartForUser("user101")).thenReturn(cart);

        orderService.placeOrder("user101");

        assertThat(SeedData.orders).hasSize(1);
    }

    @Test
    void shouldThrow400WhenCartIsNull() {
        when(cartService.getCartForUser("user101")).thenReturn(null);

        assertThatThrownBy(() -> orderService.placeOrder("user101"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Cart is empty or not found");
    }

    @Test
    void shouldThrow400WhenCartIsEmpty() {
        cart.setProducts(new ArrayList<>());
        when(cartService.getCartForUser("user101")).thenReturn(cart);

        assertThatThrownBy(() -> orderService.placeOrder("user101"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Cart is empty or not found");
    }

    @Test
    void shouldGetOrdersByUser() {
        when(cartService.getCartForUser("user101")).thenReturn(cart);
        orderService.placeOrder("user101");

        List<Order> orders = orderService.getOrdersByUser("user101");

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getUserId()).isEqualTo("user101");
    }

    @Test
    void shouldReturnEmptyListWhenNoOrdersForUser() {
        List<Order> orders = orderService.getOrdersByUser("user101");

        assertThat(orders).isEmpty();
    }

    @Test
    void shouldCancelOrderSuccessfully() {
        when(cartService.getCartForUser("user101")).thenReturn(cart);
        Order placed = orderService.placeOrder("user101");

        Order cancelled = orderService.cancelOrder(placed.getOrderId(), "user101");

        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void shouldThrow404WhenOrderNotFound() {
        assertThatThrownBy(() -> orderService.cancelOrder("order-nonexistent", "user101"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Order not found");
    }

    @Test
    void shouldThrow403WhenOrderDoesNotBelongToUser() {
        when(cartService.getCartForUser("user101")).thenReturn(cart);
        Order placed = orderService.placeOrder("user101");

        assertThatThrownBy(() -> orderService.cancelOrder(placed.getOrderId(), "user999"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Order does not belong to the user");
    }

    @Test
    void shouldThrow400WhenCancellingDeliveredOrder() {
        when(cartService.getCartForUser("user101")).thenReturn(cart);
        Order placed = orderService.placeOrder("user101");
        placed.setStatus(OrderStatus.DELIVERED);

        assertThatThrownBy(() -> orderService.cancelOrder(placed.getOrderId(), "user101"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Order cannot be cancelled in its current status");
    }

    @Test
    void shouldThrow400WhenCancellingOutForDeliveryOrder() {
        when(cartService.getCartForUser("user101")).thenReturn(cart);
        Order placed = orderService.placeOrder("user101");
        placed.setStatus(OrderStatus.OUT_FOR_DELIVERY);

        assertThatThrownBy(() -> orderService.cancelOrder(placed.getOrderId(), "user101"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Order cannot be cancelled in its current status");
    }

    @Test
    void shouldAllowCancellingPreparingOrder() {
        when(cartService.getCartForUser("user101")).thenReturn(cart);
        Order placed = orderService.placeOrder("user101");
        placed.setStatus(OrderStatus.PREPARING);

        Order cancelled = orderService.cancelOrder(placed.getOrderId(), "user101");

        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
}
