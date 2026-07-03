package com.tw.joi.delivery.dto.request;

import com.tw.joi.delivery.domain.OrderStatus;

public record UpdateOrderStatusRequest(OrderStatus status) {
}
