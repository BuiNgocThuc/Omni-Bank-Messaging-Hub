package com.example.notificationservice.client;

public interface NotificationClient {

    void send(String userId, String message);
}
