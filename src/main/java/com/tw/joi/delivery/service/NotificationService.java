package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.Notification;
import com.tw.joi.delivery.seedData.SeedData;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {

    public List<Notification> getNotificationsForUser(String userId) {
        return SeedData.notifications.stream()
            .filter(n -> userId.equals(n.getUserId()))
            .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
            .toList();
    }

    public Notification markAsRead(String notificationId, String userId) {
        Notification notification = SeedData.notifications.stream()
            .filter(n -> notificationId.equals(n.getNotificationId()))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (!userId.equals(notification.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Notification does not belong to the user");
        }

        notification.setRead(true);
        return notification;
    }

    public List<Notification> markAllAsRead(String userId) {
        List<Notification> userNotifications = SeedData.notifications.stream()
            .filter(n -> userId.equals(n.getUserId()))
            .toList();

        userNotifications.forEach(n -> n.setRead(true));

        return userNotifications.stream()
            .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
            .toList();
    }

    public Map<String, Long> getUnreadCount(String userId) {
        long count = SeedData.notifications.stream()
            .filter(n -> userId.equals(n.getUserId()) && !n.isRead())
            .count();
        return Map.of("unreadCount", count);
    }

}
