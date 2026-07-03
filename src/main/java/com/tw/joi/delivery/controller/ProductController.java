package com.tw.joi.delivery.controller;

import com.tw.joi.delivery.domain.GroceryProduct;
import com.tw.joi.delivery.dto.response.ProductResponse;
import com.tw.joi.delivery.dto.response.ProductSearchResponse;
import com.tw.joi.delivery.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProductsByStore(
        @RequestParam(name = "storeId") String storeId) {
        log.debug("GET /products storeId={}", storeId);
        return ResponseEntity.ok(productService.getProductsByStore(storeId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductSearchResponse>> searchProducts(
        @RequestParam(name = "query") String query) {
        log.debug("GET /products/search query={}", query);
        return ResponseEntity.ok(productService.searchProducts(query));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<GroceryProduct> getProductById(
        @PathVariable String productId,
        @RequestParam(name = "storeId") String storeId) {
        log.debug("GET /products/{} storeId={}", productId, storeId);
        return ResponseEntity.ok(productService.getProductDetail(productId, storeId));
    }
}
