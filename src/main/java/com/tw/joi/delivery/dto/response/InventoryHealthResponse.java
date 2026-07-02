package com.tw.joi.delivery.dto.response;

import java.util.List;

public record InventoryHealthResponse(
    String storeId,
    String storeName,
    String overallStatus,
    List<ProductStockInfo> products
) {

    public record ProductStockInfo(
        String productId,
        String productName,
        int availableStock,
        int threshold,
        String stockStatus
    ) {}
}
