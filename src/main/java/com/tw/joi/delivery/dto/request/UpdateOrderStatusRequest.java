package com.tw.joi.delivery.dto.request;

import com.tw.joi.delivery.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
    @NotNull(message = "status is required") OrderStatus status
) {
}
