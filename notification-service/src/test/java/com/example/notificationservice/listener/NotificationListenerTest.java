package com.example.notificationservice.listener;

import com.example.common.dto.message.NotificationEvent;
import com.example.notificationservice.service.NotificationService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyBoolean;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

    @InjectMocks
    private NotificationListener notificationListener;

    @Mock
    private NotificationService notificationService;

    @Mock
    private Channel channel;

    @Test
    @DisplayName("Should call notificationService.notify and ack when event is valid")
    void onTransactionCompleted_success_shouldAck() throws Exception {
        // Arrange
        NotificationEvent event = buildSuccessEvent();
        long deliveryTag = 1L;

        // Act
        notificationListener.onTransactionCompleted(event, channel, deliveryTag);

        // Assert
        verify(notificationService).notify(event);
        verify(channel).basicAck(deliveryTag, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    @DisplayName("Should nack when notificationService throws exception")
    void onTransactionCompleted_failure_shouldNack() throws Exception {
        // Arrange
        NotificationEvent event = buildSuccessEvent();
        long deliveryTag = 2L;
        doThrow(new RuntimeException("FCM unavailable"))
                .when(notificationService).notify(event);

        // Act
        notificationListener.onTransactionCompleted(event, channel, deliveryTag);

        // Assert
        verify(notificationService).notify(event);
        verify(channel).basicNack(deliveryTag, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    private NotificationEvent buildSuccessEvent() {
        return NotificationEvent.builder()
                .txId("FX-UUID-12345")
                .ownerId("user-001")
                .status("SUCCESS")
                .baseCurrency("USD")
                .targetCurrency("VND")
                .sourceAmount(new BigDecimal("500"))
                .convertedAmount(new BigDecimal("12725000"))
                .timestamp(Instant.now())
                .build();
    }
}
