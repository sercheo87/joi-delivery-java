package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.Notification;
import com.tw.joi.delivery.seedData.SeedData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class NotificationService {

    public List<Notification> getNotificationsForUser(String userId) {
        List<Notification> result = SeedData.notifications.stream()
            .filter(n -> userId.equals(n.getUserId()))
            .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
            .toList();
        log.debug("Notifications retrieved: userId={} count={}", userId, result.size());
        return result;
    }

    public Notification markAsRead(String notificationId, String userId) {
        Notification notification = SeedData.notifications.stream()
            .filter(n -> notificationId.equals(n.getNotificationId()))
            .findFirst()
            .orElseThrow(() -> {
                log.warn("Mark-read failed — notification not found: notificationId={}", notificationId);
                return new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found");
            });

        if (!userId.equals(notification.getUserId())) {
            log.warn("Mark-read rejected — notification does not belong to user: notificationId={} requestedBy={} owner={}",
                notificationId, userId, notification.getUserId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Notification does not belong to the user");
        }

        notification.setRead(true);
        log.info("Notification marked as read: notificationId={} userId={}", notificationId, userId);
        return notification;
    }

    public List<Notification> markAllAsRead(String userId) {
        List<Notification> userNotifications = SeedData.notifications.stream()
            .filter(n -> userId.equals(n.getUserId()))
            .toList();

        userNotifications.forEach(n -> n.setRead(true));
        log.info("All notifications marked as read: userId={} count={}", userId, userNotifications.size());

        return userNotifications.stream()
            .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
            .toList();
    }

    public Map<String, Long> getUnreadCount(String userId) {
        long count = SeedData.notifications.stream()
            .filter(n -> userId.equals(n.getUserId()) && !n.isRead())
            .count();
        log.debug("Unread count: userId={} count={}", userId, count);
        return Map.of("unreadCount", count);
    }
}
