package com.example.notificationservice.service;

import com.example.common.dto.message.NotificationEvent;

public interface NotificationService {

    void notify(NotificationEvent event);
}
