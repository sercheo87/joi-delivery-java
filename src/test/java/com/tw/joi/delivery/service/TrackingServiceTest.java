package com.tw.joi.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tw.joi.delivery.domain.Order;
import com.tw.joi.delivery.domain.OrderStatus;
import com.tw.joi.delivery.domain.TrackingEvent;
import com.tw.joi.delivery.seedData.SeedData;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class TrackingServiceTest {

    private TrackingService trackingService;

    private Order order;

    @BeforeEach
    void setUp() {
        SeedData.orders.clear();
        SeedData.trackingEvents.clear();

        trackingService = new TrackingService();

        order = Order.builder()
            .orderId("order-abc123")
            .userId("user101")
            .outletId("store101")
            .products(new ArrayList<>())
            .status(OrderStatus.CONFIRMED)
            .totalAmount(BigDecimal.valueOf(10.5))
            .placedAt(LocalDateTime.now())
            .estimatedDeliveryTime(LocalDateTime.now().plusMinutes(45))
            .build();

        SeedData.orders.add(order);
    }

    @Test
    void shouldReturnEmptyHistoryWhenNoEvents() {
        List<TrackingEvent> history = trackingService.getTrackingHistory("order-abc123");

        assertThat(history).isEmpty();
    }

    @Test
    void shouldReturnAllEventsForOrder() {
        TrackingEvent event1 = TrackingEvent.builder()
            .eventId("event-1")
            .orderId("order-abc123")
            .status(OrderStatus.PREPARING)
            .message("Order is being prepared")
            .timestamp(LocalDateTime.now().minusMinutes(10))
            .build();

        TrackingEvent event2 = TrackingEvent.builder()
            .eventId("event-2")
            .orderId("order-abc123")
            .status(OrderStatus.OUT_FOR_DELIVERY)
            .message("Order is out for delivery")
            .timestamp(LocalDateTime.now())
            .build();

        SeedData.trackingEvents.add(event1);
        SeedData.trackingEvents.add(event2);

        List<TrackingEvent> history = trackingService.getTrackingHistory("order-abc123");

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getEventId()).isEqualTo("event-1");
        assertThat(history.get(1).getEventId()).isEqualTo("event-2");
    }

    @Test
    void shouldOnlyReturnEventsForSpecifiedOrder() {
        TrackingEvent event1 = TrackingEvent.builder()
            .eventId("event-1")
            .orderId("order-abc123")
            .status(OrderStatus.PREPARING)
            .message("Order is being prepared")
            .timestamp(LocalDateTime.now())
            .build();

        TrackingEvent event2 = TrackingEvent.builder()
            .eventId("event-2")
            .orderId("order-other")
            .status(OrderStatus.PREPARING)
            .message("Other order is being prepared")
            .timestamp(LocalDateTime.now())
            .build();

        SeedData.trackingEvents.add(event1);
        SeedData.trackingEvents.add(event2);

        List<TrackingEvent> history = trackingService.getTrackingHistory("order-abc123");

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getEventId()).isEqualTo("event-1");
    }

    @Test
    void shouldThrow404WhenOrderNotFoundForHistory() {
        assertThatThrownBy(() -> trackingService.getTrackingHistory("order-nonexistent"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Order not found");
    }

    @Test
    void shouldReturnSyntheticEventWhenNoTrackingEventsExist() {
        TrackingEvent latest = trackingService.getLatestStatus("order-abc123");

        assertThat(latest).isNotNull();
        assertThat(latest.getOrderId()).isEqualTo("order-abc123");
        assertThat(latest.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(latest.getMessage()).contains("CONFIRMED");
    }

    @Test
    void shouldReturnLatestTrackingEvent() {
        TrackingEvent event1 = TrackingEvent.builder()
            .eventId("event-1")
            .orderId("order-abc123")
            .status(OrderStatus.PREPARING)
            .message("Order is being prepared")
            .timestamp(LocalDateTime.now().minusMinutes(10))
            .build();

        TrackingEvent event2 = TrackingEvent.builder()
            .eventId("event-2")
            .orderId("order-abc123")
            .status(OrderStatus.OUT_FOR_DELIVERY)
            .message("Order is out for delivery")
            .timestamp(LocalDateTime.now())
            .build();

        SeedData.trackingEvents.add(event1);
        SeedData.trackingEvents.add(event2);

        TrackingEvent latest = trackingService.getLatestStatus("order-abc123");

        assertThat(latest.getEventId()).isEqualTo("event-2");
        assertThat(latest.getStatus()).isEqualTo(OrderStatus.OUT_FOR_DELIVERY);
    }

    @Test
    void shouldThrow404WhenOrderNotFoundForLatestStatus() {
        assertThatThrownBy(() -> trackingService.getLatestStatus("order-nonexistent"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Order not found");
    }
}
