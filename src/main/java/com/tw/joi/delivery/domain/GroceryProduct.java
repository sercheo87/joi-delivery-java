package com.tw.joi.delivery.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroceryProduct extends Product {

    private Float sellingPrice;
    private Float weight;

    private int expiryDate;

    private int threshold;

    private int availableStock;

    private float discount;

    private GroceryStore store;

    @Builder
    public GroceryProduct(String productId, String productName, Float mrp, Cart cart,
                          Float sellingPrice, Float weight, int expiryDate, int threshold,
                          int availableStock, GroceryStore store, float discount) {
        super(productId, productName,  mrp);
        this.sellingPrice = sellingPrice;
        this.weight = weight;
        this.expiryDate = expiryDate;
        this.threshold = threshold;
        this.availableStock = availableStock;
        this.store = store;
        this.discount = discount;
    }

}
