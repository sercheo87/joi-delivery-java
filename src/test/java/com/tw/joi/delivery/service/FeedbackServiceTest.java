package com.tw.joi.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tw.joi.delivery.domain.Feedback;
import com.tw.joi.delivery.domain.FeedbackType;
import com.tw.joi.delivery.domain.Order;
import com.tw.joi.delivery.domain.OrderStatus;
import com.tw.joi.delivery.seedData.SeedData;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class FeedbackServiceTest {

    private FeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        SeedData.orders.clear();
        SeedData.feedbacks.clear();
        SeedData.notifications.clear();

        feedbackService = new FeedbackService(new UserService());
    }

    private Order buildDeliveredOrder(String orderId, String userId) {
        return Order.builder()
            .orderId(orderId)
            .userId(userId)
            .outletId("store101")
            .products(new ArrayList<>())
            .status(OrderStatus.DELIVERED)
            .totalAmount(BigDecimal.valueOf(50.0))
            .placedAt(LocalDateTime.now().minusHours(2))
            .estimatedDeliveryTime(LocalDateTime.now().minusHours(1))
            .build();
    }

    private Order buildOrderWithStatus(String orderId, String userId, OrderStatus status) {
        return Order.builder()
            .orderId(orderId)
            .userId(userId)
            .outletId("store101")
            .products(new ArrayList<>())
            .status(status)
            .totalAmount(BigDecimal.valueOf(50.0))
            .placedAt(LocalDateTime.now().minusHours(2))
            .estimatedDeliveryTime(LocalDateTime.now().minusHours(1))
            .build();
    }

    // ── submitFeedback ───────────────────────────────────────────────────────

    @Test
    void shouldSubmitFeedbackOnDeliveredOrder() {
        Order order = buildDeliveredOrder("order-delivered", "user101");
        SeedData.orders.add(order);

        Feedback feedback = feedbackService.submitFeedback(
            "user101", "order-delivered", FeedbackType.ORDER, 5, "Great service!");

        assertThat(feedback).isNotNull();
        assertThat(feedback.getFeedbackId()).isNotNull();
        assertThat(feedback.getUserId()).isEqualTo("user101");
        assertThat(feedback.getOrderId()).isEqualTo("order-delivered");
        assertThat(feedback.getType()).isEqualTo(FeedbackType.ORDER);
        assertThat(feedback.getRating()).isEqualTo(5);
        assertThat(feedback.getComment()).isEqualTo("Great service!");
        assertThat(feedback.getSubmittedAt()).isNotNull();
        assertThat(SeedData.feedbacks).hasSize(1);
    }

    @Test
    void shouldSubmitAppFeedbackWithNoOrderId() {
        Feedback feedback = feedbackService.submitFeedback(
            "user101", null, FeedbackType.APP, 4, "Love the app!");

        assertThat(feedback).isNotNull();
        assertThat(feedback.getFeedbackId()).isNotNull();
        assertThat(feedback.getUserId()).isEqualTo("user101");
        assertThat(feedback.getOrderId()).isNull();
        assertThat(feedback.getType()).isEqualTo(FeedbackType.APP);
        assertThat(feedback.getRating()).isEqualTo(4);
        assertThat(feedback.getComment()).isEqualTo("Love the app!");
        assertThat(SeedData.feedbacks).hasSize(1);
    }

    @Test
    void shouldThrow400WhenRatingIsTooLow() {
        assertThatThrownBy(() ->
            feedbackService.submitFeedback("user101", null, FeedbackType.APP, 0, "Bad"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Rating must be between 1 and 5");
    }

    @Test
    void shouldThrow400WhenRatingIsTooHigh() {
        assertThatThrownBy(() ->
            feedbackService.submitFeedback("user101", null, FeedbackType.APP, 6, "Too high"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Rating must be between 1 and 5");
    }

    @Test
    void shouldThrow400WhenOrderIsNotDelivered() {
        Order order = buildOrderWithStatus("order-preparing", "user101", OrderStatus.PREPARING);
        SeedData.orders.add(order);

        assertThatThrownBy(() ->
            feedbackService.submitFeedback("user101", "order-preparing", FeedbackType.ORDER, 4, "Early rating"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Can only rate delivered orders");
    }

    @Test
    void shouldThrow403WhenOrderBelongsToAnotherUser() {
        // Order belongs to "user-other"; user101 (who exists) tries to rate it → 403
        Order order = buildDeliveredOrder("order-delivered", "user-other");
        SeedData.orders.add(order);

        assertThatThrownBy(() ->
            feedbackService.submitFeedback("user101", "order-delivered", FeedbackType.ORDER, 5, "Good"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Order does not belong to the user");
    }

    @Test
    void shouldThrow404WhenUserNotFound() {
        assertThatThrownBy(() ->
            feedbackService.submitFeedback("user-nonexistent", null, FeedbackType.APP, 3, "Hmm"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("User not found");
    }

    @Test
    void shouldThrow404WhenOrderNotFoundDuringSubmit() {
        assertThatThrownBy(() ->
            feedbackService.submitFeedback("user101", "order-nonexistent", FeedbackType.ORDER, 5, "Good"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Order not found");
    }

    // ── getFeedbackByUser ────────────────────────────────────────────────────

    @Test
    void shouldReturnFeedbackForUserMostRecentFirst() throws InterruptedException {
        Order order1 = buildDeliveredOrder("order-1", "user101");
        Order order2 = buildDeliveredOrder("order-2", "user101");
        SeedData.orders.add(order1);
        SeedData.orders.add(order2);

        Feedback first = feedbackService.submitFeedback("user101", "order-1", FeedbackType.ORDER, 3, "OK");
        Thread.sleep(10);
        Feedback second = feedbackService.submitFeedback("user101", "order-2", FeedbackType.DELIVERY, 5, "Fast!");

        List<Feedback> result = feedbackService.getFeedbackByUser("user101");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getFeedbackId()).isEqualTo(second.getFeedbackId());
        assertThat(result.get(1).getFeedbackId()).isEqualTo(first.getFeedbackId());
    }

    @Test
    void shouldReturnEmptyListWhenNoFeedbackForUser() {
        List<Feedback> result = feedbackService.getFeedbackByUser("user101");
        assertThat(result).isEmpty();
    }

    // ── getFeedbackByOrder ───────────────────────────────────────────────────

    @Test
    void shouldReturnFeedbackForOrder() {
        Order order = buildDeliveredOrder("order-delivered", "user101");
        SeedData.orders.add(order);

        feedbackService.submitFeedback("user101", "order-delivered", FeedbackType.ORDER, 5, "Excellent!");
        feedbackService.submitFeedback("user101", "order-delivered", FeedbackType.DELIVERY, 4, "Quick!");

        List<Feedback> result = feedbackService.getFeedbackByOrder("order-delivered");

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(f -> "order-delivered".equals(f.getOrderId()));
    }

    @Test
    void shouldThrow404WhenOrderNotFoundForGetByOrder() {
        assertThatThrownBy(() ->
            feedbackService.getFeedbackByOrder("order-nonexistent"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Order not found");
    }

    // ── getAverageRating ─────────────────────────────────────────────────────

    @Test
    void shouldComputeAverageRatingForStore() {
        Order order1 = Order.builder()
            .orderId("order-s1").userId("user101").outletId("store101")
            .products(new ArrayList<>()).status(OrderStatus.DELIVERED)
            .totalAmount(BigDecimal.valueOf(20.0)).placedAt(LocalDateTime.now().minusHours(3))
            .estimatedDeliveryTime(LocalDateTime.now().minusHours(2)).build();
        Order order2 = Order.builder()
            .orderId("order-s2").userId("user101").outletId("store101")
            .products(new ArrayList<>()).status(OrderStatus.DELIVERED)
            .totalAmount(BigDecimal.valueOf(30.0)).placedAt(LocalDateTime.now().minusHours(3))
            .estimatedDeliveryTime(LocalDateTime.now().minusHours(2)).build();
        SeedData.orders.add(order1);
        SeedData.orders.add(order2);

        feedbackService.submitFeedback("user101", "order-s1", FeedbackType.ORDER, 4, "Good");
        feedbackService.submitFeedback("user101", "order-s2", FeedbackType.ORDER, 5, "Great");

        Map<String, Object> result = feedbackService.getAverageRating("store101");

        assertThat(result.get("storeId")).isEqualTo("store101");
        assertThat(result.get("averageRating")).isEqualTo(4.5);
        assertThat(result.get("totalReviews")).isEqualTo(2L);
    }

    @Test
    void shouldReturnZeroAverageWhenNoReviews() {
        Map<String, Object> result = feedbackService.getAverageRating("store101");

        assertThat(result.get("storeId")).isEqualTo("store101");
        assertThat(result.get("averageRating")).isEqualTo(0.0);
        assertThat(result.get("totalReviews")).isEqualTo(0L);
    }
}
