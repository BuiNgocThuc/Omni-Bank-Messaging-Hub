package com.example.sellforeignprocessorservice.publisher;

import com.example.common.constant.RabbitMQConstants;
import com.example.common.dto.message.NotificationEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventPublisherTest {

    @InjectMocks
    private NotificationEventPublisher publisher;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("Should send event with correct exchange, routing key and payload")
    void publishTransactionResult_shouldSendToCorrectDestination() {
        // Arrange
        NotificationEvent event = NotificationEvent.builder()
                .txId("FX-UUID-999")
                .ownerId("user-555")
                .status("SUCCESS")
                .baseCurrency("USD")
                .targetCurrency("VND")
                .sourceAmount(new BigDecimal("100"))
                .convertedAmount(new BigDecimal("2545000"))
                .timestamp(Instant.now())
                .build();

        // Act
        publisher.publishTransactionResult(event);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                RabbitMQConstants.TOPIC_EXCHANGE,
                RabbitMQConstants.ROUTING_NOTIFICATION,
                event
        );
    }

    @Test
    @DisplayName("Should send failed event with failure reason")
    void publishTransactionResult_failed_shouldSendWithReason() {
        // Arrange
        NotificationEvent event = NotificationEvent.builder()
                .txId("FX-UUID-888")
                .ownerId("user-333")
                .status("FAILED")
                .baseCurrency("JPY")
                .targetCurrency("VND")
                .sourceAmount(new BigDecimal("50000"))
                .failureReason("Hold rejected")
                .timestamp(Instant.now())
                .build();

        // Act
        publisher.publishTransactionResult(event);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                RabbitMQConstants.TOPIC_EXCHANGE,
                RabbitMQConstants.ROUTING_NOTIFICATION,
                event
        );
    }
}
