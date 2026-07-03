package com.tw.joi.delivery.controller;

import com.tw.joi.delivery.domain.Notification;
import com.tw.joi.delivery.service.NotificationService;
import com.tw.joi.delivery.service.SseNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SseNotificationService sseNotificationService;

    @GetMapping
    public ResponseEntity<List<Notification>> getNotificationsForUser(
        @RequestParam(name = "userId") String userId
    ) {
        log.debug("GET /notifications userId={}", userId);
        return ResponseEntity.ok(notificationService.getNotificationsForUser(userId));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
        @RequestParam(name = "userId") String userId
    ) {
        log.debug("GET /notifications/unread-count userId={}", userId);
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Notification> markAsRead(
        @PathVariable String notificationId,
        @RequestParam(name = "userId") String userId
    ) {
        log.info("PATCH /notifications/{}/read userId={}", notificationId, userId);
        return ResponseEntity.ok(notificationService.markAsRead(notificationId, userId));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<List<Notification>> markAllAsRead(
        @RequestParam(name = "userId") String userId
    ) {
        log.info("PATCH /notifications/read-all userId={}", userId);
        return ResponseEntity.ok(notificationService.markAllAsRead(userId));
    }

    @GetMapping("/stream")
    public SseEmitter stream(@RequestParam(name = "userId") String userId) {
        log.info("GET /notifications/stream userId={}", userId);
        return sseNotificationService.subscribe(userId);
    }
}
