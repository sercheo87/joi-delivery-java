package com.tw.joi.delivery.controller;

import com.tw.joi.delivery.domain.TrackingEvent;
import com.tw.joi.delivery.service.TrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    @GetMapping("/{orderId}")
    public ResponseEntity<List<TrackingEvent>> getTrackingHistory(@PathVariable String orderId) {
        log.debug("GET /tracking/{}", orderId);
        return ResponseEntity.ok(trackingService.getTrackingHistory(orderId));
    }

    @GetMapping("/{orderId}/status")
    public ResponseEntity<TrackingEvent> getLatestStatus(@PathVariable String orderId) {
        log.debug("GET /tracking/{}/status", orderId);
        return ResponseEntity.ok(trackingService.getLatestStatus(orderId));
    }
}
