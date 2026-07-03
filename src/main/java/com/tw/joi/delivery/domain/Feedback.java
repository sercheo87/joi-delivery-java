package com.tw.joi.delivery.domain;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Feedback {

    private String feedbackId;
    private String userId;
    private String orderId;       // nullable — feedback can be general (APP type) or order-specific
    private FeedbackType type;    // ORDER, DELIVERY, PRODUCT, APP
    private int rating;           // 1–5
    private String comment;       // optional free text
    private LocalDateTime submittedAt;

}
