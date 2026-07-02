package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.Cart;
import com.tw.joi.delivery.domain.Notification;
import com.tw.joi.delivery.domain.Order;
import com.tw.joi.delivery.domain.OrderStatus;
import com.tw.joi.delivery.domain.Product;
import com.tw.joi.delivery.domain.TrackingEvent;
import com.tw.joi.delivery.seedData.SeedData;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

        Notification confirmation = Notification.builder()
            .notificationId(UUID.randomUUID().toString())
            .userId(userId)
            .orderId(order.getOrderId())
            .title("Order Confirmed")
            .message("Your order #" + order.getOrderId() + " has been confirmed and is being prepared.")
            .read(false)
            .createdAt(LocalDateTime.now())
            .build();
        SeedData.notifications.add(confirmation);

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

    private static final Map<OrderStatus, OrderStatus> VALID_TRANSITIONS = Map.of(
        OrderStatus.CONFIRMED, OrderStatus.PREPARING,
        OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY,
        OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED
    );

    private static final Map<OrderStatus, String> STATUS_MESSAGES = Map.of(
        OrderStatus.CONFIRMED, "Order has been confirmed and is awaiting preparation",
        OrderStatus.PREPARING, "Order is being prepared",
        OrderStatus.OUT_FOR_DELIVERY, "Order is out for delivery",
        OrderStatus.DELIVERED, "Order has been delivered",
        OrderStatus.CANCELLED, "Order has been cancelled"
    );

    private static final Map<OrderStatus, String> NOTIFICATION_TITLES = Map.of(
        OrderStatus.PREPARING, "Order Being Prepared",
        OrderStatus.OUT_FOR_DELIVERY, "Order Out for Delivery",
        OrderStatus.DELIVERED, "Order Delivered",
        OrderStatus.CANCELLED, "Order Cancelled"
    );

    private static final Map<OrderStatus, String> NOTIFICATION_MESSAGES = Map.of(
        OrderStatus.PREPARING, "Your order #%s is now being prepared.",
        OrderStatus.OUT_FOR_DELIVERY, "Your order #%s is out for delivery and will arrive soon.",
        OrderStatus.DELIVERED, "Your order #%s has been delivered. Enjoy!",
        OrderStatus.CANCELLED, "Your order #%s has been cancelled."
    );

    public Order updateOrderStatus(String orderId, String userId, OrderStatus newStatus) {
        Order order = SeedData.orders.stream()
            .filter(o -> orderId.equals(o.getOrderId()))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!userId.equals(order.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order does not belong to the user");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot update a cancelled order");
        }

        OrderStatus expectedNext = VALID_TRANSITIONS.get(order.getStatus());
        if (expectedNext == null || expectedNext != newStatus) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid status transition from " + order.getStatus() + " to " + newStatus);
        }

        order.setStatus(newStatus);

        TrackingEvent event = TrackingEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .orderId(orderId)
            .status(newStatus)
            .message(STATUS_MESSAGES.getOrDefault(newStatus, newStatus.name()))
            .timestamp(LocalDateTime.now())
            .build();
        SeedData.trackingEvents.add(event);

        String notifTitle = NOTIFICATION_TITLES.getOrDefault(newStatus, newStatus.name());
        String notifMessageTemplate = NOTIFICATION_MESSAGES.getOrDefault(newStatus, "Your order #%s status has been updated to " + newStatus.name() + ".");
        Notification notification = Notification.builder()
            .notificationId(UUID.randomUUID().toString())
            .userId(order.getUserId())
            .orderId(orderId)
            .title(notifTitle)
            .message(String.format(notifMessageTemplate, orderId))
            .read(false)
            .createdAt(LocalDateTime.now())
            .build();
        SeedData.notifications.add(notification);

        return order;
    }
}
