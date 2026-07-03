package com.tw.joi.delivery.dto.request;

import com.tw.joi.delivery.domain.PaymentMethod;

public record InitiatePaymentRequest(String orderId, String userId, PaymentMethod method) {}
