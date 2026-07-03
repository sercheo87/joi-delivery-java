package com.tw.joi.delivery.service;

import com.tw.joi.delivery.event.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseNotificationService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(userId, emitter);
        emitter.onCompletion(() -> {
            emitters.remove(userId);
            log.debug("SSE connection closed: userId={}", userId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(userId);
            log.debug("SSE connection timed out: userId={}", userId);
        });
        emitter.onError(e -> {
            emitters.remove(userId);
            log.debug("SSE connection error: userId={} error={}", userId, e.getMessage());
        });
        log.info("SSE subscription registered: userId={}", userId);
        return emitter;
    }

    @EventListener
    public void onNotification(NotificationEvent event) {
        String userId = event.notification().getUserId();
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            log.debug("No SSE subscriber for userId={}, skipping push", userId);
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                .name("notification")
                .data(event.notification()));
            log.info("SSE event pushed: userId={} notificationId={} title={}", userId, event.notification().getNotificationId(), event.notification().getTitle());
        } catch (IOException e) {
            emitters.remove(userId);
            log.warn("SSE push failed — connection removed: userId={} error={}", userId, e.getMessage());
        }
    }
}
