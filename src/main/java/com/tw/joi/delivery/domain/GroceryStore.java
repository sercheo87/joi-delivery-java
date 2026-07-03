package com.tw.joi.delivery.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;


@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GroceryStore extends Outlet {

    private Set<GroceryProduct> inventory = new HashSet<>();

    @Builder
    public GroceryStore(String name, String description, String outletId) {
        super(name, description, outletId);
        this.inventory = new HashSet<>();
    }

}
