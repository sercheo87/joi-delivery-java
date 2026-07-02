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
