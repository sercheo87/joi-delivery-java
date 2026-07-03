package com.tw.joi.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tw.joi.delivery.domain.Notification;
import com.tw.joi.delivery.domain.Order;
import com.tw.joi.delivery.domain.OrderStatus;
import com.tw.joi.delivery.domain.Payment;
import com.tw.joi.delivery.domain.PaymentMethod;
import com.tw.joi.delivery.domain.PaymentStatus;
import com.tw.joi.delivery.seedData.SeedData;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class PaymentServiceTest {

    private PaymentService paymentService;

    // "order-2".hashCode() % 10 != 0  →  card/UPI payment succeeds
    private static final String SUCCESS_ORDER_ID = "order-2";
    // "order-1".hashCode() % 10 == 0  →  card/UPI payment fails
    private static final String FAIL_ORDER_ID = "order-1";

    @BeforeEach
    void setUp() {
        SeedData.orders.clear();
        SeedData.payments.clear();
        SeedData.notifications.clear();
        SeedData.idempotencyStore.clear();

        paymentService = new PaymentService();
    }

    private Order buildOrder(String orderId, String userId, OrderStatus status) {
        return Order.builder()
            .orderId(orderId)
            .userId(userId)
            .outletId("store101")
            .products(new ArrayList<>())
            .status(status)
            .totalAmount(BigDecimal.valueOf(50.0))
            .placedAt(LocalDateTime.now())
            .estimatedDeliveryTime(LocalDateTime.now().plusMinutes(45))
            .build();
    }

    // ── initiatePayment ──────────────────────────────────────────────────────

    @Test
    void shouldProcessCodPaymentSuccessfully() {
        Order order = buildOrder("order-cod", "user101", OrderStatus.CONFIRMED);
        SeedData.orders.add(order);

        Payment payment = paymentService.initiatePayment("order-cod", "user101", PaymentMethod.CASH_ON_DELIVERY, "key-cod");

        assertThat(payment).isNotNull();
        assertThat(payment.getPaymentId()).isNotNull();
        assertThat(payment.getOrderId()).isEqualTo("order-cod");
        assertThat(payment.getUserId()).isEqualTo("user101");
        assertThat(payment.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(50.0));
        assertThat(payment.getMethod()).isEqualTo(PaymentMethod.CASH_ON_DELIVERY);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getInitiatedAt()).isNotNull();
        assertThat(payment.getCompletedAt()).isNotNull();
        assertThat(payment.getFailureReason()).isNull();
    }

    @Test
    void shouldProcessCardPaymentSuccessfully() {
        Order order = buildOrder(SUCCESS_ORDER_ID, "user101", OrderStatus.CONFIRMED);
        SeedData.orders.add(order);

        Payment payment = paymentService.initiatePayment(SUCCESS_ORDER_ID, "user101", PaymentMethod.CREDIT_CARD, "key-success");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getCompletedAt()).isNotNull();
        assertThat(payment.getFailureReason()).isNull();
    }

    @Test
    void shouldFailCardPaymentWhenHashConditionMet() {
        Order order = buildOrder(FAIL_ORDER_ID, "user101", OrderStatus.CONFIRMED);
        SeedData.orders.add(order);

        Payment payment = paymentService.initiatePayment(FAIL_ORDER_ID, "user101", PaymentMethod.CREDIT_CARD, "key-fail");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getCompletedAt()).isNotNull();
        assertThat(payment.getFailureReason()).isNotBlank();
    }

    @Test
    void shouldFailUpiPaymentWhenHashConditionMet() {
        Order order = buildOrder(FAIL_ORDER_ID, "user101", OrderStatus.CONFIRMED);
        SeedData.orders.add(order);

        Payment payment = paymentService.initiatePayment(FAIL_ORDER_ID, "user101", PaymentMethod.UPI, "key-fail-upi");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureReason()).isNotBlank();
    }

    @Test
    void shouldAddPaymentToSeedData() {
        Order order = buildOrder("order-cod", "user101", OrderStatus.CONFIRMED);
        SeedData.orders.add(order);

        paymentService.initiatePayment("order-cod", "user101", PaymentMethod.CASH_ON_DELIVERY, "key-cod");

        assertThat(SeedData.payments).hasSize(1);
    }

    @Test
    void shouldCreateSuccessNotificationOnSuccessfulPayment() {
        Order order = buildOrder(SUCCESS_ORDER_ID, "user101", OrderStatus.CONFIRMED);
        SeedData.orders.add(order);

        paymentService.initiatePayment(SUCCESS_ORDER_ID, "user101", PaymentMethod.CREDIT_CARD, "key-success");

        assertThat(SeedData.notifications).hasSize(1);
        Notification notification = SeedData.notifications.get(0);
        assertThat(notification.getTitle()).isEqualTo("Payment Successful");
        assertThat(notification.getUserId()).isEqualTo("user101");
        assertThat(notification.getOrderId()).isEqualTo(SUCCESS_ORDER_ID);
        assertThat(notification.isRead()).isFalse();
    }

    @Test
    void shouldCreateFailedNotificationOnFailedPayment() {
        Order order = buildOrder(FAIL_ORDER_ID, "user101", OrderStatus.CONFIRMED);
        SeedData.orders.add(order);

        paymentService.initiatePayment(FAIL_ORDER_ID, "user101", PaymentMethod.DEBIT_CARD, "key-fail-debit");

        assertThat(SeedData.notifications).hasSize(1);
        Notification notification = SeedData.notifications.get(0);
        assertThat(notification.getTitle()).isEqualTo("Payment Failed");
        assertThat(notification.getUserId()).isEqualTo("user101");
    }

    @Test
    void shouldThrow404WhenOrderNotFound() {
        assertThatThrownBy(() ->
            paymentService.initiatePayment("order-nonexistent", "user101", PaymentMethod.CASH_ON_DELIVERY, "key-404"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Order not found");
    }

    @Test
    void shouldThrow403WhenOrderBelongsToAnotherUser() {
        Order order = buildOrder("order-cod", "user101", OrderStatus.CONFIRMED);
        SeedData.orders.add(order);

        assertThatThrownBy(() ->
            paymentService.initiatePayment("order-cod", "user999", PaymentMethod.CASH_ON_DELIVERY, "key-403"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Order does not belong to the user");
    }

    @Test
    void shouldThrow400WhenOrderIsCancelled() {
        Order order = buildOrder("order-cod", "user101", OrderStatus.CANCELLED);
        SeedData.orders.add(order);

        assertThatThrownBy(() ->
            paymentService.initiatePayment("order-cod", "user101", PaymentMethod.CASH_ON_DELIVERY, "key-cod"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Cannot pay for a cancelled order");
    }

    @Test
    void shouldThrow400WhenOrderAlreadyPaid() {
        Order order = buildOrder(SUCCESS_ORDER_ID, "user101", OrderStatus.CONFIRMED);
        SeedData.orders.add(order);

        paymentService.initiatePayment(SUCCESS_ORDER_ID, "user101", PaymentMethod.CREDIT_CARD, "key-success");

        assertThatThrownBy(() ->
            paymentService.initiatePayment(SUCCESS_ORDER_ID, "user101", PaymentMethod.DEBIT_CARD, "key-success-2"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Order has already been paid");
    }

    // ── getPaymentByOrder ────────────────────────────────────────────────────

    @Test
    void shouldGetPaymentByOrder() {
        Order order = buildOrder(SUCCESS_ORDER_ID, "user101", OrderStatus.CONFIRMED);
        SeedData.orders.add(order);

        paymentService.initiatePayment(SUCCESS_ORDER_ID, "user101", PaymentMethod.CASH_ON_DELIVERY, "key-get");

        Payment retrieved = paymentService.getPaymentByOrder(SUCCESS_ORDER_ID, "user101");

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getOrderId()).isEqualTo(SUCCESS_ORDER_ID);
        assertThat(retrieved.getUserId()).isEqualTo("user101");
    }

    @Test
    void shouldReturnMostRecentPaymentWhenMultipleExist() {
        Order order = buildOrder(FAIL_ORDER_ID, "user101", OrderStatus.CONFIRMED);
        SeedData.orders.add(order);

        Payment first = paymentService.initiatePayment(FAIL_ORDER_ID, "user101", PaymentMethod.CREDIT_CARD, "key-fail");

        Payment second = Payment.builder()
            .paymentId("payment-second")
            .orderId(FAIL_ORDER_ID)
            .userId("user101")
            .amount(BigDecimal.valueOf(50.0))
            .method(PaymentMethod.CASH_ON_DELIVERY)
            .status(PaymentStatus.SUCCESS)
            .initiatedAt(first.getInitiatedAt().plusSeconds(5))
            .completedAt(first.getInitiatedAt().plusSeconds(5))
            .build();
        SeedData.payments.add(second);

        Payment retrieved = paymentService.getPaymentByOrder(FAIL_ORDER_ID, "user101");

        assertThat(retrieved.getPaymentId()).isEqualTo("payment-second");
    }

    @Test
    void shouldThrow404WhenNoPaymentForOrder() {
        assertThatThrownBy(() ->
            paymentService.getPaymentByOrder("order-nonexistent", "user101"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Payment not found for order");
    }

    @Test
    void shouldThrow403WhenGettingPaymentBelongingToAnotherUser() {
        Order order = buildOrder(SUCCESS_ORDER_ID, "user101", OrderStatus.CONFIRMED);
        SeedData.orders.add(order);

        paymentService.initiatePayment(SUCCESS_ORDER_ID, "user101", PaymentMethod.CASH_ON_DELIVERY, "key-403-get");

        assertThatThrownBy(() ->
            paymentService.getPaymentByOrder(SUCCESS_ORDER_ID, "user999"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Payment does not belong to the user");
    }

    // ── refundPayment ────────────────────────────────────────────────────────

    @Test
    void shouldRefundSuccessfulPayment() {
        Order order = buildOrder(SUCCESS_ORDER_ID, "user101", OrderStatus.CONFIRMED);
        SeedData.orders.add(order);

        Payment payment = paymentService.initiatePayment(SUCCESS_ORDER_ID, "user101", PaymentMethod.CASH_ON_DELIVERY, "key-refund");

        Payment refunded = paymentService.refundPayment(payment.getPaymentId(), "user101");

        assertThat(refunded.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(refunded.getCompletedAt()).isNotNull();
    }

    @Test
    void shouldCreateRefundNotification() {
        Order order = buildOrder(SUCCESS_ORDER_ID, "user101", OrderStatus.CONFIRMED);
        SeedData.orders.add(order);

        Payment payment = paymentService.initiatePayment(SUCCESS_ORDER_ID, "user101", PaymentMethod.CASH_ON_DELIVERY, "key-refund-notif");
        SeedData.notifications.clear();

        paymentService.refundPayment(payment.getPaymentId(), "user101");

        assertThat(SeedData.notifications).hasSize(1);
        assertThat(SeedData.notifications.get(0).getTitle()).isEqualTo("Payment Refunded");
        assertThat(SeedData.notifications.get(0).getUserId()).isEqualTo("user101");
    }

    @Test
    void shouldThrow404WhenRefundingNonExistentPayment() {
        assertThatThrownBy(() ->
            paymentService.refundPayment("payment-nonexistent", "user101"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Payment not found");
    }

    @Test
    void shouldThrow403WhenRefundingPaymentBelongingToAnotherUser() {
        Order order = buildOrder(SUCCESS_ORDER_ID, "user101", OrderStatus.CONFIRMED);
        SeedData.orders.add(order);

        Payment payment = paymentService.initiatePayment(SUCCESS_ORDER_ID, "user101", PaymentMethod.CASH_ON_DELIVERY, "key-403-refund");

        assertThatThrownBy(() ->
            paymentService.refundPayment(payment.getPaymentId(), "user999"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Payment does not belong to the user");
    }

    @Test
    void shouldThrow400WhenRefundingFailedPayment() {
        Order order = buildOrder(FAIL_ORDER_ID, "user101", OrderStatus.CONFIRMED);
        SeedData.orders.add(order);

        Payment payment = paymentService.initiatePayment(FAIL_ORDER_ID, "user101", PaymentMethod.CREDIT_CARD, "key-fail");

        assertThatThrownBy(() ->
            paymentService.refundPayment(payment.getPaymentId(), "user101"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Only successful payments can be refunded");
    }

    // ── idempotency ──────────────────────────────────────────────────────────

    @Test
    void shouldReturnSamePaymentWhenIdempotencyKeyIsReused() {
        Order order = buildOrder(SUCCESS_ORDER_ID, "user101", OrderStatus.CONFIRMED);
        SeedData.orders.add(order);

        Payment first = paymentService.initiatePayment(SUCCESS_ORDER_ID, "user101", PaymentMethod.CASH_ON_DELIVERY, "key-idem");
        Payment second = paymentService.initiatePayment(SUCCESS_ORDER_ID, "user101", PaymentMethod.CASH_ON_DELIVERY, "key-idem");

        assertThat(second.getPaymentId()).isEqualTo(first.getPaymentId());
        assertThat(SeedData.payments).hasSize(1);
    }

    @Test
    void shouldCreateNewPaymentForDifferentIdempotencyKey() {
        Order order1 = buildOrder("order-a", "user101", OrderStatus.CONFIRMED);
        Order order2 = buildOrder("order-b", "user101", OrderStatus.CONFIRMED);
        SeedData.orders.add(order1);
        SeedData.orders.add(order2);

        Payment first = paymentService.initiatePayment("order-a", "user101", PaymentMethod.CASH_ON_DELIVERY, "key-a");
        Payment second = paymentService.initiatePayment("order-b", "user101", PaymentMethod.CASH_ON_DELIVERY, "key-b");

        assertThat(second.getPaymentId()).isNotEqualTo(first.getPaymentId());
        assertThat(SeedData.payments).hasSize(2);
    }

    @Test
    void shouldThrow400WhenRefundingAlreadyRefundedPayment() {
        Order order = buildOrder(SUCCESS_ORDER_ID, "user101", OrderStatus.CONFIRMED);
        SeedData.orders.add(order);

        Payment payment = paymentService.initiatePayment(SUCCESS_ORDER_ID, "user101", PaymentMethod.CASH_ON_DELIVERY, "key-double-refund");
        paymentService.refundPayment(payment.getPaymentId(), "user101");

        assertThatThrownBy(() ->
            paymentService.refundPayment(payment.getPaymentId(), "user101"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Only successful payments can be refunded");
    }
}
