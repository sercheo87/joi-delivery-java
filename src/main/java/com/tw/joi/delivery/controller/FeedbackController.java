package com.tw.joi.delivery.controller;

import com.tw.joi.delivery.domain.Feedback;
import com.tw.joi.delivery.dto.request.SubmitFeedbackRequest;
import com.tw.joi.delivery.service.FeedbackService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<Feedback> submitFeedback(@RequestBody SubmitFeedbackRequest request) {
        Feedback feedback = feedbackService.submitFeedback(
            request.userId(), request.orderId(), request.type(), request.rating(), request.comment());
        return ResponseEntity.status(HttpStatus.CREATED).body(feedback);
    }

    @GetMapping("/user")
    public ResponseEntity<List<Feedback>> getFeedbackByUser(@RequestParam(name = "userId") String userId) {
        return ResponseEntity.ok(feedbackService.getFeedbackByUser(userId));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<Feedback>> getFeedbackByOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(feedbackService.getFeedbackByOrder(orderId));
    }

    @GetMapping("/store/{storeId}/rating")
    public ResponseEntity<Map<String, Object>> getAverageRating(@PathVariable String storeId) {
        return ResponseEntity.ok(feedbackService.getAverageRating(storeId));
    }
}
