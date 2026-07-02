package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.GroceryProduct;
import com.tw.joi.delivery.dto.response.ProductResponse;
import com.tw.joi.delivery.dto.response.ProductSearchResponse;
import com.tw.joi.delivery.seedData.SeedData;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final List<GroceryProduct> products = SeedData.groceryProducts;
    private final StoreService storeService;

    public GroceryProduct getProduct(String productId, String outletId) {
        return products.stream()
            .filter(groceryProduct ->
                        groceryProduct.getProductId().equals(productId)
                            && groceryProduct.getStore().getOutletId().equals(outletId))
            .findFirst()
            .orElse(null);
    }

    public List<ProductResponse> getProductsByStore(String storeId) {
        storeService.findById(storeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found: " + storeId));
        return products.stream()
            .filter(p -> p.getStore().getOutletId().equals(storeId))
            .map(p -> new ProductResponse(p.getProductId(), p.getProductName(), p.getMrp(), p.getAvailableStock()))
            .toList();
    }

    public List<GroceryProduct> getGroceryProductsByStore(String storeId) {
        storeService.findById(storeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found: " + storeId));
        return products.stream()
            .filter(p -> p.getStore().getOutletId().equals(storeId))
            .toList();
    }

    public List<ProductSearchResponse> searchProducts(String query) {
        return products.stream()
            .filter(p -> p.getProductName().toLowerCase().contains(query.toLowerCase()))
            .map(p -> new ProductSearchResponse(p.getProductId(), p.getProductName(), p.getMrp(),
                                                p.getAvailableStock(), p.getStore().getOutletId()))
            .toList();
    }

    public GroceryProduct getProductDetail(String productId, String storeId) {
        storeService.findById(storeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found: " + storeId));
        return products.stream()
            .filter(p -> p.getProductId().equals(productId) && p.getStore().getOutletId().equals(storeId))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + productId));
    }
}
