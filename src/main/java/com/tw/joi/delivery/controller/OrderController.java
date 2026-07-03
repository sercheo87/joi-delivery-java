package com.tw.joi.delivery.controller;

import com.tw.joi.delivery.domain.Order;
import com.tw.joi.delivery.dto.request.UpdateOrderStatusRequest;
import com.tw.joi.delivery.dto.response.PlaceOrderResponse;
import com.tw.joi.delivery.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/place")
    public ResponseEntity<PlaceOrderResponse> placeOrder(@RequestParam(name = "userId") String userId) {
        log.info("POST /orders/place userId={}", userId);
        Order order = orderService.placeOrder(userId);
        PlaceOrderResponse response = new PlaceOrderResponse(
            order.getOrderId(),
            order.getUserId(),
            order.getOutletId(),
            order.getProducts(),
            order.getStatus(),
            order.getTotalAmount(),
            order.getPlacedAt(),
            order.getEstimatedDeliveryTime()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<Order>> getOrdersByUser(@RequestParam(name = "userId") String userId) {
        log.info("GET /orders userId={}", userId);
        return ResponseEntity.ok(orderService.getOrdersByUser(userId));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Order> cancelOrder(
        @PathVariable String orderId,
        @RequestParam(name = "userId") String userId
    ) {
        log.info("DELETE /orders/{} userId={}", orderId, userId);
        return ResponseEntity.ok(orderService.cancelOrder(orderId, userId));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<Order> updateOrderStatus(
        @PathVariable String orderId,
        @RequestParam(name = "userId") String userId,
        @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        log.info("PATCH /orders/{}/status userId={} newStatus={}", orderId, userId, request.status());
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, userId, request.status()));
    }
}
