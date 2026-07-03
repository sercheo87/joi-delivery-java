package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.GroceryProduct;
import com.tw.joi.delivery.dto.response.ProductResponse;
import com.tw.joi.delivery.dto.response.ProductSearchResponse;
import com.tw.joi.delivery.seedData.SeedData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final List<GroceryProduct> products = SeedData.groceryProducts;
    private final StoreService storeService;

    public GroceryProduct getProduct(String productId, String outletId) {
        return products.stream()
            .filter(p -> p.getProductId().equals(productId) && p.getStore().getOutletId().equals(outletId))
            .findFirst()
            .orElse(null);
    }

    public List<ProductResponse> getProductsByStore(String storeId) {
        storeService.findById(storeId)
            .orElseThrow(() -> {
                log.warn("Products by store failed — store not found: storeId={}", storeId);
                return new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found: " + storeId);
            });
        List<ProductResponse> result = products.stream()
            .filter(p -> p.getStore().getOutletId().equals(storeId))
            .map(p -> new ProductResponse(p.getProductId(), p.getProductName(), p.getMrp(), p.getAvailableStock()))
            .toList();
        log.debug("Products listed: storeId={} count={}", storeId, result.size());
        return result;
    }

    public List<GroceryProduct> getGroceryProductsByStore(String storeId) {
        storeService.findById(storeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found: " + storeId));
        return products.stream()
            .filter(p -> p.getStore().getOutletId().equals(storeId))
            .toList();
    }

    public List<ProductSearchResponse> searchProducts(String query) {
        List<ProductSearchResponse> result = products.stream()
            .filter(p -> p.getProductName().toLowerCase().contains(query.toLowerCase()))
            .map(p -> new ProductSearchResponse(p.getProductId(), p.getProductName(), p.getMrp(),
                p.getAvailableStock(), p.getStore().getOutletId()))
            .toList();
        log.debug("Product search: query='{}' hits={}", query, result.size());
        return result;
    }

    public GroceryProduct getProductDetail(String productId, String storeId) {
        storeService.findById(storeId)
            .orElseThrow(() -> {
                log.warn("Product detail failed — store not found: storeId={}", storeId);
                return new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found: " + storeId);
            });
        return products.stream()
            .filter(p -> p.getProductId().equals(productId) && p.getStore().getOutletId().equals(storeId))
            .findFirst()
            .orElseThrow(() -> {
                log.warn("Product detail failed — product not found: productId={} storeId={}", productId, storeId);
                return new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + productId);
            });
    }
}
