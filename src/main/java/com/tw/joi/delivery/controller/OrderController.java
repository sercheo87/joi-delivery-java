package com.tw.joi.delivery.controller;

import com.tw.joi.delivery.domain.Order;
import com.tw.joi.delivery.dto.response.PlaceOrderResponse;
import com.tw.joi.delivery.service.OrderService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/place")
    public ResponseEntity<PlaceOrderResponse> placeOrder(@RequestParam(name = "userId") String userId) {
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
        return ResponseEntity.ok(orderService.getOrdersByUser(userId));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Order> cancelOrder(
        @PathVariable String orderId,
        @RequestParam(name = "userId") String userId
    ) {
        return ResponseEntity.ok(orderService.cancelOrder(orderId, userId));
    }
}
