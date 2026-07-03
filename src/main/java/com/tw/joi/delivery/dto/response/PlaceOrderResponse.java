package com.tw.joi.delivery.dto.response;

import com.tw.joi.delivery.domain.OrderStatus;
import com.tw.joi.delivery.domain.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PlaceOrderResponse(
    String orderId,
    String userId,
    String outletId,
    List<Product> products,
    OrderStatus status,
    BigDecimal totalAmount,
    LocalDateTime placedAt,
    LocalDateTime estimatedDeliveryTime
) {
}
