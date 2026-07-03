package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.GroceryStore;
import com.tw.joi.delivery.seedData.SeedData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class StoreService {

    private final List<GroceryStore> stores = SeedData.groceryStores;

    public Optional<GroceryStore> findById(String storeId) {
        Optional<GroceryStore> store = stores.stream()
            .filter(s -> s.getOutletId().equals(storeId))
            .findFirst();
        if (store.isEmpty()) {
            log.debug("Store not found: storeId={}", storeId);
        }
        return store;
    }
}
