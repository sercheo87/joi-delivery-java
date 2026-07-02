package com.tw.joi.delivery.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    private String orderId;
    private String userId;
    private String outletId;
    private List<Product> products;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime placedAt;
    private LocalDateTime estimatedDeliveryTime;

}
