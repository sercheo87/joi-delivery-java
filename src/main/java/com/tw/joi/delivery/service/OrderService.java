package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.*;
import com.tw.joi.delivery.seedData.SeedData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

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
    private final CartService cartService;

    public Order placeOrder(String userId) {
        Cart cart = cartService.getCartForUser(userId);
        if (cart == null || cart.getProducts() == null || cart.getProducts().isEmpty()) {
            log.warn("Place order rejected — cart empty or not found: userId={}", userId);
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

        // Reserve stock: decrement availableStock for each product
        order.getProducts().forEach(p -> {
            if (p instanceof GroceryProduct gp) {
                int before = gp.getAvailableStock();
                gp.setAvailableStock(before - 1);
                log.info("Stock reserved: productId={} stock {}→{}", gp.getProductId(), before, gp.getAvailableStock());
            }
        });

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

        log.info("Order placed: orderId={} userId={} outletId={} totalAmount={} items={}", order.getOrderId(), userId, order.getOutletId(), totalAmount, order.getProducts().size());
        return order;
    }

    public List<Order> getOrdersByUser(String userId) {
        List<Order> orders = SeedData.orders.stream()
            .filter(order -> userId.equals(order.getUserId()))
            .toList();
        log.debug("Orders retrieved: userId={} count={}", userId, orders.size());
        return orders;
    }

    public Order cancelOrder(String orderId, String userId) {
        Order order = SeedData.orders.stream()
            .filter(o -> orderId.equals(o.getOrderId()))
            .findFirst()
            .orElseThrow(() -> {
                log.warn("Cancel order failed — order not found: orderId={}", orderId);
                return new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
            });

        if (!userId.equals(order.getUserId())) {
            log.warn("Cancel order rejected — order does not belong to user: orderId={} requestedBy={} owner={}", orderId, userId, order.getUserId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order does not belong to the user");
        }

        if (order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.PREPARING) {
            log.warn("Cancel order rejected — invalid status: orderId={} currentStatus={}", orderId, order.getStatus());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order cannot be cancelled in its current status");
        }

        order.setStatus(OrderStatus.CANCELLED);

        // Release reserved stock (compensating transaction)
        order.getProducts().forEach(p -> {
            if (p instanceof GroceryProduct gp) {
                int before = gp.getAvailableStock();
                gp.setAvailableStock(before + 1);
                log.info("Stock released: productId={} stock {}→{}", gp.getProductId(), before, gp.getAvailableStock());
            }
        });

        SeedData.notifications.add(Notification.builder()
            .notificationId(UUID.randomUUID().toString())
            .userId(order.getUserId())
            .orderId(orderId)
            .title("Order Cancelled")
            .message(String.format("Your order #%s has been cancelled.", orderId))
            .read(false)
            .createdAt(LocalDateTime.now())
            .build());

        log.info("Order cancelled: orderId={} userId={}", orderId, userId);
        return order;
    }

    public Order updateOrderStatus(String orderId, String userId, OrderStatus newStatus) {
        Order order = SeedData.orders.stream()
            .filter(o -> orderId.equals(o.getOrderId()))
            .findFirst()
            .orElseThrow(() -> {
                log.warn("Update status failed — order not found: orderId={}", orderId);
                return new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
            });

        if (!userId.equals(order.getUserId())) {
            log.warn("Update status rejected — order does not belong to user: orderId={} requestedBy={} owner={}", orderId, userId, order.getUserId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order does not belong to the user");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.warn("Update status rejected — order is cancelled: orderId={}", orderId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot update a cancelled order");
        }

        OrderStatus expectedNext = VALID_TRANSITIONS.get(order.getStatus());
        if (expectedNext == null || expectedNext != newStatus) {
            log.warn("Update status rejected — invalid transition: orderId={} from={} to={}", orderId, order.getStatus(), newStatus);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid status transition from " + order.getStatus() + " to " + newStatus);
        }

        OrderStatus previousStatus = order.getStatus();
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

        log.info("Order status updated: orderId={} {}→{} userId={}", orderId, previousStatus, newStatus, userId);
        return order;
    }
}
