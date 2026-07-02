package com.tw.joi.delivery.dto.response;

import java.math.BigDecimal;

public record ProductSearchResponse(String productId, String productName, BigDecimal mrp, int availableStock, String storeId) {
}
