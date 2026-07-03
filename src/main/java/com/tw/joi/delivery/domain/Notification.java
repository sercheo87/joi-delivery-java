package com.tw.joi.delivery.domain;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    private String notificationId;
    private String userId;
    private String orderId;
    private String title;
    private String message;
    @Builder.Default
    private boolean read = false;
    private LocalDateTime createdAt;

}
