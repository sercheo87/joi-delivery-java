package com.tw.joi.delivery.controller;

import com.tw.joi.delivery.domain.GroceryProduct;
import com.tw.joi.delivery.dto.response.ProductResponse;
import com.tw.joi.delivery.dto.response.ProductSearchResponse;
import com.tw.joi.delivery.service.ProductService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProductsByStore(
            @RequestParam(name = "storeId") String storeId) {
        return ResponseEntity.ok(productService.getProductsByStore(storeId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductSearchResponse>> searchProducts(
            @RequestParam(name = "query") String query) {
        return ResponseEntity.ok(productService.searchProducts(query));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<GroceryProduct> getProductById(
            @PathVariable String productId,
            @RequestParam(name = "storeId") String storeId) {
        return ResponseEntity.ok(productService.getProductDetail(productId, storeId));
    }
}
