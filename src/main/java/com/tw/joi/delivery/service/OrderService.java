package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.Cart;
import com.tw.joi.delivery.domain.Order;
import com.tw.joi.delivery.domain.OrderStatus;
import com.tw.joi.delivery.domain.Product;
import com.tw.joi.delivery.seedData.SeedData;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartService cartService;

    public Order placeOrder(String userId) {
        Cart cart = cartService.getCartForUser(userId);
        if (cart == null || cart.getProducts() == null || cart.getProducts().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty or not found");
        }

        LocalDateTime now = LocalDateTime.now();
        BigDecimal totalAmount = cart.getProducts().stream()
            .map(Product::getMrp)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
            .orderId("order-" + UUID.randomUUID().toString().substring(0, 8))
            .userId(userId)
            .outletId(cart.getOutlet().getOutletId())
            .products(new ArrayList<>(cart.getProducts()))
            .status(OrderStatus.CONFIRMED)
            .totalAmount(totalAmount)
            .placedAt(now)
            .estimatedDeliveryTime(now.plusMinutes(45))
            .build();

        SeedData.orders.add(order);
        cart.setProducts(new ArrayList<>());

        return order;
    }

    public List<Order> getOrdersByUser(String userId) {
        return SeedData.orders.stream()
            .filter(order -> userId.equals(order.getUserId()))
            .toList();
    }

    public Order cancelOrder(String orderId, String userId) {
        Order order = SeedData.orders.stream()
            .filter(o -> orderId.equals(o.getOrderId()))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!userId.equals(order.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order does not belong to the user");
        }

        if (order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.PREPARING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order cannot be cancelled in its current status");
        }

        order.setStatus(OrderStatus.CANCELLED);
        return order;
    }
}
