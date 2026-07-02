package com.tw.joi.delivery.controller;

import com.tw.joi.delivery.domain.Notification;
import com.tw.joi.delivery.service.NotificationService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<Notification>> getNotificationsForUser(
        @RequestParam(name = "userId") String userId
    ) {
        return ResponseEntity.ok(notificationService.getNotificationsForUser(userId));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
        @RequestParam(name = "userId") String userId
    ) {
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Notification> markAsRead(
        @PathVariable String notificationId,
        @RequestParam(name = "userId") String userId
    ) {
        return ResponseEntity.ok(notificationService.markAsRead(notificationId, userId));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<List<Notification>> markAllAsRead(
        @RequestParam(name = "userId") String userId
    ) {
        return ResponseEntity.ok(notificationService.markAllAsRead(userId));
    }

}
