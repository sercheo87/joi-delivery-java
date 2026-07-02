package com.tw.joi.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tw.joi.delivery.domain.Notification;
import com.tw.joi.delivery.seedData.SeedData;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class NotificationServiceTest {

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        SeedData.notifications.clear();
        notificationService = new NotificationService();
    }

    private Notification buildNotification(String id, String userId, String orderId,
                                           String title, String message, boolean read,
                                           LocalDateTime createdAt) {
        return Notification.builder()
            .notificationId(id)
            .userId(userId)
            .orderId(orderId)
            .title(title)
            .message(message)
            .read(read)
            .createdAt(createdAt)
            .build();
    }

    // getNotificationsForUser tests

    @Test
    void shouldReturnEmptyListWhenNoNotificationsForUser() {
        List<Notification> result = notificationService.getNotificationsForUser("user101");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnOnlyNotificationsForGivenUser() {
        LocalDateTime now = LocalDateTime.now();
        SeedData.notifications.add(buildNotification("n1", "user101", "order-1",
            "Order Confirmed", "Your order #order-1 has been confirmed.", false, now));
        SeedData.notifications.add(buildNotification("n2", "user102", "order-2",
            "Order Confirmed", "Your order #order-2 has been confirmed.", false, now));

        List<Notification> result = notificationService.getNotificationsForUser("user101");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNotificationId()).isEqualTo("n1");
    }

    @Test
    void shouldReturnNotificationsMostRecentFirst() {
        LocalDateTime earlier = LocalDateTime.now().minusMinutes(5);
        LocalDateTime later = LocalDateTime.now();
        SeedData.notifications.add(buildNotification("n1", "user101", "order-1",
            "Order Confirmed", "msg1", false, earlier));
        SeedData.notifications.add(buildNotification("n2", "user101", "order-1",
            "Order Being Prepared", "msg2", false, later));

        List<Notification> result = notificationService.getNotificationsForUser("user101");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getNotificationId()).isEqualTo("n2");
        assertThat(result.get(1).getNotificationId()).isEqualTo("n1");
    }

    // markAsRead tests

    @Test
    void shouldMarkNotificationAsRead() {
        SeedData.notifications.add(buildNotification("n1", "user101", "order-1",
            "Order Confirmed", "msg", false, LocalDateTime.now()));

        Notification result = notificationService.markAsRead("n1", "user101");

        assertThat(result.isRead()).isTrue();
    }

    @Test
    void shouldReturnUpdatedNotificationAfterMarkAsRead() {
        SeedData.notifications.add(buildNotification("n1", "user101", "order-1",
            "Order Confirmed", "msg", false, LocalDateTime.now()));

        Notification result = notificationService.markAsRead("n1", "user101");

        assertThat(result.getNotificationId()).isEqualTo("n1");
        assertThat(result.getUserId()).isEqualTo("user101");
        assertThat(result.isRead()).isTrue();
    }

    @Test
    void shouldThrow404WhenNotificationNotFound() {
        assertThatThrownBy(() -> notificationService.markAsRead("nonexistent-id", "user101"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Notification not found");
    }

    @Test
    void shouldThrow403WhenNotificationDoesNotBelongToUser() {
        SeedData.notifications.add(buildNotification("n1", "user101", "order-1",
            "Order Confirmed", "msg", false, LocalDateTime.now()));

        assertThatThrownBy(() -> notificationService.markAsRead("n1", "user999"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Notification does not belong to the user");
    }

    // markAllAsRead tests

    @Test
    void shouldMarkAllNotificationsAsReadForUser() {
        LocalDateTime now = LocalDateTime.now();
        SeedData.notifications.add(buildNotification("n1", "user101", "order-1",
            "Order Confirmed", "msg1", false, now.minusMinutes(2)));
        SeedData.notifications.add(buildNotification("n2", "user101", "order-1",
            "Order Being Prepared", "msg2", false, now.minusMinutes(1)));
        SeedData.notifications.add(buildNotification("n3", "user102", "order-2",
            "Order Confirmed", "msg3", false, now));

        List<Notification> result = notificationService.markAllAsRead("user101");

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(Notification::isRead);
        // notification for user102 should remain unread
        Notification user102Notif = SeedData.notifications.stream()
            .filter(n -> "n3".equals(n.getNotificationId()))
            .findFirst().orElseThrow();
        assertThat(user102Notif.isRead()).isFalse();
    }

    @Test
    void shouldReturnEmptyListWhenMarkAllAsReadForUserWithNoNotifications() {
        List<Notification> result = notificationService.markAllAsRead("user101");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnNotificationsMostRecentFirstAfterMarkAllAsRead() {
        LocalDateTime now = LocalDateTime.now();
        SeedData.notifications.add(buildNotification("n1", "user101", "order-1",
            "Order Confirmed", "msg1", false, now.minusMinutes(5)));
        SeedData.notifications.add(buildNotification("n2", "user101", "order-1",
            "Order Being Prepared", "msg2", false, now));

        List<Notification> result = notificationService.markAllAsRead("user101");

        assertThat(result.get(0).getNotificationId()).isEqualTo("n2");
        assertThat(result.get(1).getNotificationId()).isEqualTo("n1");
    }

    // getUnreadCount tests

    @Test
    void shouldReturnUnreadCountForUser() {
        LocalDateTime now = LocalDateTime.now();
        SeedData.notifications.add(buildNotification("n1", "user101", "order-1",
            "Order Confirmed", "msg1", false, now));
        SeedData.notifications.add(buildNotification("n2", "user101", "order-1",
            "Order Being Prepared", "msg2", true, now));
        SeedData.notifications.add(buildNotification("n3", "user102", "order-2",
            "Order Confirmed", "msg3", false, now));

        Map<String, Long> result = notificationService.getUnreadCount("user101");

        assertThat(result).containsKey("unreadCount");
        assertThat(result.get("unreadCount")).isEqualTo(1L);
    }

    @Test
    void shouldReturnZeroUnreadCountWhenNoNotifications() {
        Map<String, Long> result = notificationService.getUnreadCount("user101");

        assertThat(result.get("unreadCount")).isEqualTo(0L);
    }

    @Test
    void shouldReturnZeroUnreadCountAfterMarkAllAsRead() {
        SeedData.notifications.add(buildNotification("n1", "user101", "order-1",
            "Order Confirmed", "msg", false, LocalDateTime.now()));

        notificationService.markAllAsRead("user101");
        Map<String, Long> result = notificationService.getUnreadCount("user101");

        assertThat(result.get("unreadCount")).isEqualTo(0L);
    }

}
