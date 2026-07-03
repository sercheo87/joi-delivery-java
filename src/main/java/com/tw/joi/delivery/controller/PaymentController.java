package com.tw.joi.delivery.controller;

import com.tw.joi.delivery.domain.Payment;
import com.tw.joi.delivery.dto.request.InitiatePaymentRequest;
import com.tw.joi.delivery.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    public ResponseEntity<Payment> initiatePayment(
        @RequestHeader("X-Idempotency-Key") String idempotencyKey,
        @RequestBody InitiatePaymentRequest request) {
        log.info("POST /payments/initiate orderId={} userId={} method={} idempotencyKey={}",
            request.orderId(), request.userId(), request.method(), idempotencyKey);
        Payment payment = paymentService.initiatePayment(request.orderId(), request.userId(), request.method(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Payment> getPaymentByOrder(
        @PathVariable String orderId,
        @RequestParam(name = "userId") String userId
    ) {
        log.info("GET /payments/order/{} userId={}", orderId, userId);
        return ResponseEntity.ok(paymentService.getPaymentByOrder(orderId, userId));
    }

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<Payment> refundPayment(
        @PathVariable String paymentId,
        @RequestParam(name = "userId") String userId
    ) {
        log.info("POST /payments/{}/refund userId={}", paymentId, userId);
        return ResponseEntity.ok(paymentService.refundPayment(paymentId, userId));
    }
}
