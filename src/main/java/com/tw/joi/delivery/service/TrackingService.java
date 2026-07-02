package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.TrackingEvent;
import com.tw.joi.delivery.seedData.SeedData;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TrackingService {

    public List<TrackingEvent> getTrackingHistory(String orderId) {
        boolean orderExists = SeedData.orders.stream()
            .anyMatch(o -> orderId.equals(o.getOrderId()));

        if (!orderExists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }

        return SeedData.trackingEvents.stream()
            .filter(e -> orderId.equals(e.getOrderId()))
            .sorted(Comparator.comparing(TrackingEvent::getTimestamp))
            .toList();
    }

    public TrackingEvent getLatestStatus(String orderId) {
        var order = SeedData.orders.stream()
            .filter(o -> orderId.equals(o.getOrderId()))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        return SeedData.trackingEvents.stream()
            .filter(e -> orderId.equals(e.getOrderId()))
            .max(Comparator.comparing(TrackingEvent::getTimestamp))
            .orElseGet(() -> TrackingEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .status(order.getStatus())
                .message("Current order status: " + order.getStatus().name())
                .timestamp(order.getPlacedAt() != null ? order.getPlacedAt() : LocalDateTime.now())
                .build());
    }
}
