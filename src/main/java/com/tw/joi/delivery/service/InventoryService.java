package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.GroceryProduct;
import com.tw.joi.delivery.domain.GroceryStore;
import com.tw.joi.delivery.dto.response.InventoryHealthResponse;
import com.tw.joi.delivery.dto.response.InventoryHealthResponse.ProductStockInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final StoreService storeService;
    private final ProductService productService;

    public InventoryHealthResponse getInventoryHealth(String storeId) {
        GroceryStore store = storeService.findById(storeId)
            .orElseThrow(() -> {
                log.warn("Inventory health check failed — store not found: storeId={}", storeId);
                return new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found: " + storeId);
            });

        List<ProductStockInfo> productInfos = productService.getGroceryProductsByStore(storeId).stream()
            .map(this::toProductStockInfo)
            .toList();

        String overallStatus = computeOverallStatus(productInfos);
        log.debug("Inventory health: storeId={} overallStatus={} products={}", storeId, overallStatus, productInfos.size());
        return new InventoryHealthResponse(storeId, store.getName(), overallStatus, productInfos);
    }

    private ProductStockInfo toProductStockInfo(GroceryProduct product) {
        String status;
        if (product.getAvailableStock() == 0) {
            status = "OUT_OF_STOCK";
            log.warn("Product out of stock: productId={} productName={}", product.getProductId(), product.getProductName());
        } else if (product.getAvailableStock() <= product.getThreshold()) {
            status = "LOW_STOCK";
            log.warn("Product low stock: productId={} stock={} threshold={}",
                product.getProductId(), product.getAvailableStock(), product.getThreshold());
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
