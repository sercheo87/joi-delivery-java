package com.tw.joi.delivery.domain;

import java.time.LocalDateTime;
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
public class TrackingEvent {

    private String eventId;
    private String orderId;
    private OrderStatus status;
    private String message;
    private LocalDateTime timestamp;

}
