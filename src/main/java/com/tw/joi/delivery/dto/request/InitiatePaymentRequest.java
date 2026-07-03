package com.tw.joi.delivery.dto.request;

import com.tw.joi.delivery.domain.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InitiatePaymentRequest(
    @NotBlank(message = "orderId is required") String orderId,
    @NotBlank(message = "userId is required") String userId,
    @NotNull(message = "method is required") PaymentMethod method
) {
}
