package com.tw.joi.delivery.dto.request;

import com.tw.joi.delivery.domain.FeedbackType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmitFeedbackRequest(
    @NotBlank(message = "userId is required") String userId,
    String orderId,
    @NotNull(message = "type is required") FeedbackType type,
    @Min(value = 1, message = "rating must be between 1 and 5")
    @Max(value = 5, message = "rating must be between 1 and 5") int rating,
    String comment
) {
}
