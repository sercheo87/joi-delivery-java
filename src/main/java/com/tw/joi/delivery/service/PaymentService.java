package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.Notification;
import com.tw.joi.delivery.domain.Order;
import com.tw.joi.delivery.domain.OrderStatus;
import com.tw.joi.delivery.domain.Payment;
import com.tw.joi.delivery.domain.PaymentMethod;
import com.tw.joi.delivery.domain.PaymentStatus;
import com.tw.joi.delivery.seedData.SeedData;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentService {

    public Payment initiatePayment(String orderId, String userId, PaymentMethod method, String idempotencyKey) {
        Payment cached = SeedData.idempotencyStore.get(idempotencyKey);
        if (cached != null) return cached;

        Order order = SeedData.orders.stream()
            .filter(o -> orderId.equals(o.getOrderId()))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!userId.equals(order.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order does not belong to the user");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot pay for a cancelled order");
        }

        boolean alreadyPaid = SeedData.payments.stream()
            .anyMatch(p -> orderId.equals(p.getOrderId()) && PaymentStatus.SUCCESS == p.getStatus());

        if (alreadyPaid) {
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

            Notification notification = Notification.builder()
                .notificationId(UUID.randomUUID().toString())
                .userId(userId)
                .orderId(orderId)
                .title("Payment Successful")
                .message("Your payment for order #" + orderId + " was successful.")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
            SeedData.notifications.add(notification);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setCompletedAt(LocalDateTime.now());
            payment.setFailureReason("Payment processing failed");

            Notification notification = Notification.builder()
                .notificationId(UUID.randomUUID().toString())
                .userId(userId)
                .orderId(orderId)
                .title("Payment Failed")
                .message("Your payment for order #" + orderId + " has failed. Please try again.")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
            SeedData.notifications.add(notification);
        }

        SeedData.payments.add(payment);
        SeedData.idempotencyStore.put(idempotencyKey, payment);
        return payment;
    }

    public Payment getPaymentByOrder(String orderId, String userId) {
        Payment payment = SeedData.payments.stream()
            .filter(p -> orderId.equals(p.getOrderId()))
            .max(Comparator.comparing(Payment::getInitiatedAt))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found for order"));

        if (!userId.equals(payment.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Payment does not belong to the user");
        }

        return payment;
    }

    public Payment refundPayment(String paymentId, String userId) {
        Payment payment = SeedData.payments.stream()
            .filter(p -> paymentId.equals(p.getPaymentId()))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        if (!userId.equals(payment.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Payment does not belong to the user");
        }

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only successful payments can be refunded");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setCompletedAt(LocalDateTime.now());

        Notification notification = Notification.builder()
            .notificationId(UUID.randomUUID().toString())
            .userId(userId)
            .orderId(payment.getOrderId())
            .title("Payment Refunded")
            .message("Your payment for order #" + payment.getOrderId() + " has been refunded.")
            .read(false)
            .createdAt(LocalDateTime.now())
            .build();
        SeedData.notifications.add(notification);

        return payment;
    }
}
