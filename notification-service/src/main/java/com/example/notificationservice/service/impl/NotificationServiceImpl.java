package com.example.notificationservice.service.impl;

import com.example.common.dto.message.NotificationEvent;
import com.example.notificationservice.client.NotificationClient;
import com.example.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationClient notificationClient;

    @Override
    public void notify(NotificationEvent event) {

        String message = buildMessage(event);

        notificationClient.send(event.getOwnerId(), message);

        log.info("Notification sent to user [{}] for tx [{}]", event.getOwnerId(), event.getTxId());
    }

    String buildMessage(NotificationEvent event) {

        if ("SUCCESS".equals(event.getStatus())) {

            return String.format("Giao dịch bán %s %s sang %s thành công. Số tiền nhận: %s %s.",
                    event.getSourceAmount(), event.getBaseCurrency(),
                    event.getTargetCurrency(),
                    event.getConvertedAmount(), event.getTargetCurrency()
            );
        }

        return String.format("Giao dịch bán %s %s sang %s thất bại. Lý do: %s",
                event.getSourceAmount(), event.getBaseCurrency(),
                event.getTargetCurrency(),
                event.getFailureReason()
        );
    }
}
