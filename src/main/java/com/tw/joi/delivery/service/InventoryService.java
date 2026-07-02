package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.GroceryProduct;
import com.tw.joi.delivery.domain.GroceryStore;
import com.tw.joi.delivery.dto.response.InventoryHealthResponse;
import com.tw.joi.delivery.dto.response.InventoryHealthResponse.ProductStockInfo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final StoreService storeService;
    private final ProductService productService;

    public InventoryHealthResponse getInventoryHealth(String storeId) {
        GroceryStore store = storeService.findById(storeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found: " + storeId));

        List<ProductStockInfo> productInfos = productService.getProductsByStore(storeId).stream()
            .map(this::toProductStockInfo)
            .toList();

        return new InventoryHealthResponse(storeId, store.getName(), computeOverallStatus(productInfos), productInfos);
    }

    private ProductStockInfo toProductStockInfo(GroceryProduct product) {
        String status;
        if (product.getAvailableStock() == 0) {
            status = "OUT_OF_STOCK";
        } else if (product.getAvailableStock() <= product.getThreshold()) {
            status = "LOW_STOCK";
        } else {
            status = "HEALTHY";
        }
        return new ProductStockInfo(product.getProductId(), product.getProductName(),
                                    product.getAvailableStock(), product.getThreshold(), status);
    }

    private String computeOverallStatus(List<ProductStockInfo> products) {
        if (products.stream().anyMatch(p -> "OUT_OF_STOCK".equals(p.stockStatus()))) return "CRITICAL";
        if (products.stream().anyMatch(p -> "LOW_STOCK".equals(p.stockStatus()))) return "LOW_STOCK";
        return "HEALTHY";
    }
}
