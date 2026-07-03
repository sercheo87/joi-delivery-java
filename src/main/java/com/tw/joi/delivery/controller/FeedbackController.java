package com.tw.joi.delivery.controller;

import com.tw.joi.delivery.domain.Feedback;
import com.tw.joi.delivery.dto.request.SubmitFeedbackRequest;
import com.tw.joi.delivery.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<Feedback> submitFeedback(@RequestBody SubmitFeedbackRequest request) {
        log.info("POST /feedback userId={} orderId={} type={} rating={}",
            request.userId(), request.orderId(), request.type(), request.rating());
        Feedback feedback = feedbackService.submitFeedback(
            request.userId(), request.orderId(), request.type(), request.rating(), request.comment());
        return ResponseEntity.status(HttpStatus.CREATED).body(feedback);
    }

    @GetMapping("/user")
    public ResponseEntity<List<Feedback>> getFeedbackByUser(@RequestParam(name = "userId") String userId) {
        log.debug("GET /feedback/user userId={}", userId);
        return ResponseEntity.ok(feedbackService.getFeedbackByUser(userId));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<Feedback>> getFeedbackByOrder(@PathVariable String orderId) {
        log.debug("GET /feedback/order/{}", orderId);
        return ResponseEntity.ok(feedbackService.getFeedbackByOrder(orderId));
    }

    @GetMapping("/store/{storeId}/rating")
    public ResponseEntity<Map<String, Object>> getAverageRating(@PathVariable String storeId) {
        log.debug("GET /feedback/store/{}/rating", storeId);
        return ResponseEntity.ok(feedbackService.getAverageRating(storeId));
    }
}
