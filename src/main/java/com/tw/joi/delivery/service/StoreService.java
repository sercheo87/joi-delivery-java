package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.GroceryStore;
import com.tw.joi.delivery.seedData.SeedData;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class StoreService {

    private final List<GroceryStore> stores = List.of(SeedData.store101, SeedData.store102);

    public Optional<GroceryStore> findById(String storeId) {
        return stores.stream()
            .filter(store -> store.getOutletId().equals(storeId))
            .findFirst();
    }
}
