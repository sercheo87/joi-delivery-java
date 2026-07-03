package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.*;
import com.tw.joi.delivery.event.NotificationEvent;
import com.tw.joi.delivery.seedData.SeedData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.UUID;

@Slf4j
@Service
public class PaymentService {

    @Autowired(required = false)
    private ApplicationEventPublisher eventPublisher;

    public Payment initiatePayment(String orderId, String userId, PaymentMethod method, String idempotencyKey) {
        boolean[] wasNew = {false};

        // computeIfAbsent is atomic: the lambda runs at most once per key even under concurrent calls
        Payment result = SeedData.idempotencyStore.computeIfAbsent(idempotencyKey, key -> {
            wasNew[0] = true;

            Order order = SeedData.orders.stream()
                .filter(o -> orderId.equals(o.getOrderId()))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Payment rejected — order not found: orderId={}", orderId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
                });

            if (!userId.equals(order.getUserId())) {
                log.warn("Payment rejected — order does not belong to user: orderId={} requestedBy={} owner={}", orderId, userId, order.getUserId());
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order does not belong to the user");
            }

            if (order.getStatus() == OrderStatus.CANCELLED) {
                log.warn("Payment rejected — order is cancelled: orderId={}", orderId);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot pay for a cancelled order");
            }

            boolean alreadyPaid = SeedData.payments.stream()
                .anyMatch(p -> orderId.equals(p.getOrderId()) && PaymentStatus.SUCCESS == p.getStatus());

            if (alreadyPaid) {
                log.warn("Payment rejected — order already paid: orderId={}", orderId);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order has already been paid");
            }

            LocalDateTime now = LocalDateTime.now();
            Payment payment = Payment.builder()
                .paymentId(UUID.randomUUID().toString())
                .orderId(orderId)
                .userId(userId)
                .amount(order.getTotalAmount())
                .method(method)
                .status(PaymentStatus.PENDING)
                .initiatedAt(now)
                .build();

            boolean success = method == PaymentMethod.CASH_ON_DELIVERY || orderId.hashCode() % 10 != 0;

            if (success) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setCompletedAt(LocalDateTime.now());
                publishNotification(Notification.builder()
                    .notificationId(UUID.randomUUID().toString())
                    .userId(userId)
                    .orderId(orderId)
                    .title("Payment Successful")
                    .message("Your payment for order #" + orderId + " was successful.")
                    .read(false)
                    .createdAt(LocalDateTime.now())
                    .build());
                log.info("Payment successful: paymentId={} orderId={} userId={} method={} amount={}", payment.getPaymentId(), orderId, userId, method, payment.getAmount());
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setCompletedAt(LocalDateTime.now());
                payment.setFailureReason("Payment processing failed");
                publishNotification(Notification.builder()
                    .notificationId(UUID.randomUUID().toString())
                    .userId(userId)
                    .orderId(orderId)
                    .title("Payment Failed")
                    .message("Your payment for order #" + orderId + " has failed. Please try again.")
                    .read(false)
                    .createdAt(LocalDateTime.now())
                    .build());
                log.warn("Payment failed: paymentId={} orderId={} userId={} method={} reason={}", payment.getPaymentId(), orderId, userId, method, payment.getFailureReason());
            }

            SeedData.payments.add(payment);
            return payment;
        });

        if (!wasNew[0]) {
            log.warn("Idempotency hit — returning cached result: key={} paymentId={} status={}", idempotencyKey, result.getPaymentId(), result.getStatus());
        }
        return result;
    }

    public Payment getPaymentByOrder(String orderId, String userId) {
        Payment payment = SeedData.payments.stream()
            .filter(p -> orderId.equals(p.getOrderId()))
            .max(Comparator.comparing(Payment::getInitiatedAt))
            .orElseThrow(() -> {
                log.warn("Payment not found for order: orderId={}", orderId);
                return new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found for order");
            });

        if (!userId.equals(payment.getUserId())) {
            log.warn("Payment access rejected — does not belong to user: paymentId={} requestedBy={} owner={}", payment.getPaymentId(), userId, payment.getUserId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Payment does not belong to the user");
        }

        log.debug("Payment retrieved: paymentId={} orderId={} status={}", payment.getPaymentId(), orderId, payment.getStatus());
        return payment;
    }

    public Payment refundPayment(String paymentId, String userId) {
        Payment payment = SeedData.payments.stream()
            .filter(p -> paymentId.equals(p.getPaymentId()))
            .findFirst()
            .orElseThrow(() -> {
                log.warn("Refund rejected — payment not found: paymentId={}", paymentId);
                return new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found");
            });

        if (!userId.equals(payment.getUserId())) {
            log.warn("Refund rejected — payment does not belong to user: paymentId={} requestedBy={} owner={}", paymentId, userId, payment.getUserId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Payment does not belong to the user");
        }

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            log.warn("Refund rejected — payment not in SUCCESS state: paymentId={} currentStatus={}", paymentId, payment.getStatus());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only successful payments can be refunded");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setCompletedAt(LocalDateTime.now());

        publishNotification(Notification.builder()
            .notificationId(UUID.randomUUID().toString())
            .userId(userId)
            .orderId(payment.getOrderId())
            .title("Payment Refunded")
            .message("Your payment for order #" + payment.getOrderId() + " has been refunded.")
            .read(false)
            .createdAt(LocalDateTime.now())
            .build());

        log.info("Payment refunded: paymentId={} orderId={} userId={} amount={}", paymentId, payment.getOrderId(), userId, payment.getAmount());
        return payment;
    }

    private void publishNotification(Notification notification) {
        SeedData.notifications.add(notification);
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new NotificationEvent(notification));
        }
    }
}
