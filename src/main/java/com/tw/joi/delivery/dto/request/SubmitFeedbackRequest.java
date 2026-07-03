package com.tw.joi.delivery.dto.request;

import com.tw.joi.delivery.domain.FeedbackType;

public record SubmitFeedbackRequest(String userId, String orderId, FeedbackType type, int rating, String comment) {
}
