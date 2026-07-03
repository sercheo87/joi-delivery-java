package com.tw.joi.delivery.domain;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingEvent {

    private String eventId;
    private String orderId;
    private OrderStatus status;
    private String message;
    private LocalDateTime timestamp;

}
