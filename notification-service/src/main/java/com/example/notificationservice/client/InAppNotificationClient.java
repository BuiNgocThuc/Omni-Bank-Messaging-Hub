package com.example.notificationservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InAppNotificationClient implements NotificationClient {

    @Override
    public void send(String userId, String message) {

        log.info("[IN-APP] Notification to user [{}]: {}", userId, message);
    }
}
