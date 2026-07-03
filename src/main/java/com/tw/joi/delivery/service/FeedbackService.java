package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.Feedback;
import com.tw.joi.delivery.domain.FeedbackType;
import com.tw.joi.delivery.domain.Order;
import com.tw.joi.delivery.domain.OrderStatus;
import com.tw.joi.delivery.seedData.SeedData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final UserService userService;

    public Feedback submitFeedback(String userId, String orderId, FeedbackType type, int rating, String comment) {
        if (userService.fetchUserById(userId) == null) {
            log.warn("Feedback rejected — user not found: userId={}", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        if (rating < 1 || rating > 5) {
            log.warn("Feedback rejected — invalid rating: userId={} rating={}", userId, rating);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 1 and 5");
        }

        if (orderId != null && !orderId.isBlank()) {
            Order order = SeedData.orders.stream()
                .filter(o -> orderId.equals(o.getOrderId()))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Feedback rejected — order not found: orderId={}", orderId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
                });

            if (!userId.equals(order.getUserId())) {
                log.warn("Feedback rejected — order does not belong to user: orderId={} requestedBy={} owner={}", orderId, userId, order.getUserId());
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order does not belong to the user");
            }

            if (order.getStatus() != OrderStatus.DELIVERED) {
                log.warn("Feedback rejected — order not delivered: orderId={} currentStatus={}", orderId, order.getStatus());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can only rate delivered orders");
            }
        }

        Feedback feedback = Feedback.builder()
            .feedbackId(UUID.randomUUID().toString())
            .userId(userId)
            .orderId(orderId)
            .type(type)
            .rating(rating)
            .comment(comment)
            .submittedAt(LocalDateTime.now())
            .build();

        SeedData.feedbacks.add(feedback);
        log.info("Feedback submitted: feedbackId={} userId={} orderId={} type={} rating={}",
            feedback.getFeedbackId(), userId, orderId, type, rating);
        return feedback;
    }

    public List<Feedback> getFeedbackByUser(String userId) {
        List<Feedback> result = SeedData.feedbacks.stream()
            .filter(f -> userId.equals(f.getUserId()))
            .sorted(Comparator.comparing(Feedback::getSubmittedAt).reversed())
            .toList();
        log.debug("Feedback retrieved by user: userId={} count={}", userId, result.size());
        return result;
    }

    public List<Feedback> getFeedbackByOrder(String orderId) {
        boolean orderExists = SeedData.orders.stream()
            .anyMatch(o -> orderId.equals(o.getOrderId()));
        if (!orderExists) {
            log.warn("Feedback query failed — order not found: orderId={}", orderId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
        List<Feedback> result = SeedData.feedbacks.stream()
            .filter(f -> orderId.equals(f.getOrderId()))
            .toList();
        log.debug("Feedback retrieved by order: orderId={} count={}", orderId, result.size());
        return result;
    }

    public Map<String, Object> getAverageRating(String storeId) {
        List<String> orderIds = SeedData.orders.stream()
            .filter(o -> storeId.equals(o.getOutletId()))
            .map(Order::getOrderId)
            .toList();

        List<Feedback> storeFeedbacks = SeedData.feedbacks.stream()
            .filter(f -> f.getOrderId() != null && orderIds.contains(f.getOrderId()))
            .toList();

        double averageRating = storeFeedbacks.stream()
            .mapToInt(Feedback::getRating)
            .average()
            .orElse(0.0);

        double rounded = Math.round(averageRating * 10.0) / 10.0;
        log.debug("Average rating calculated: storeId={} avg={} totalReviews={}", storeId, rounded, storeFeedbacks.size());

        return Map.of(
            "storeId", storeId,
            "averageRating", rounded,
            "totalReviews", (long) storeFeedbacks.size()
        );
    }
}
