package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.TrackingEvent;
import com.tw.joi.delivery.seedData.SeedData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TrackingService {

    public List<TrackingEvent> getTrackingHistory(String orderId) {
        boolean orderExists = SeedData.orders.stream()
            .anyMatch(o -> orderId.equals(o.getOrderId()));

        if (!orderExists) {
            log.warn("Tracking history failed — order not found: orderId={}", orderId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }

        List<TrackingEvent> events = SeedData.trackingEvents.stream()
            .filter(e -> orderId.equals(e.getOrderId()))
            .sorted(Comparator.comparing(TrackingEvent::getTimestamp))
            .toList();
        log.debug("Tracking history retrieved: orderId={} events={}", orderId, events.size());
        return events;
    }

    public TrackingEvent getLatestStatus(String orderId) {
        var order = SeedData.orders.stream()
            .filter(o -> orderId.equals(o.getOrderId()))
            .findFirst()
            .orElseThrow(() -> {
                log.warn("Latest tracking status failed — order not found: orderId={}", orderId);
                return new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
            });

        TrackingEvent latest = SeedData.trackingEvents.stream()
            .filter(e -> orderId.equals(e.getOrderId()))
            .max(Comparator.comparing(TrackingEvent::getTimestamp))
            .orElseGet(() -> TrackingEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .status(order.getStatus())
                .message("Current order status: " + order.getStatus().name())
                .timestamp(order.getPlacedAt() != null ? order.getPlacedAt() : LocalDateTime.now())
                .build());

        log.debug("Latest tracking status: orderId={} status={}", orderId, latest.getStatus());
        return latest;
    }
}
