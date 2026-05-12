package com.example.notificationservice.service.impl;

import com.example.common.dto.message.NotificationEvent;
import com.example.notificationservice.client.NotificationClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Mock
    private NotificationClient notificationClient;

    @Test
    @DisplayName("Should build success message and send to correct user")
    void notify_success_shouldBuildCorrectMessage() {
        // Arrange
        NotificationEvent event = NotificationEvent.builder()
                .txId("FX-UUID-001")
                .ownerId("user-100")
                .status("SUCCESS")
                .baseCurrency("USD")
                .targetCurrency("VND")
                .sourceAmount(new BigDecimal("500"))
                .convertedAmount(new BigDecimal("12725000"))
                .timestamp(Instant.now())
                .build();

        // Act
        notificationService.notify(event);

        // Assert
        ArgumentCaptor<String> userCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationClient).send(userCaptor.capture(), messageCaptor.capture());

        assertThat(userCaptor.getValue()).isEqualTo("user-100");
        assertThat(messageCaptor.getValue())
                .contains("thành công")
                .contains("500")
                .contains("USD")
                .contains("VND")
                .contains("12725000");
    }

    @Test
    @DisplayName("Should build failure message with reason")
    void notify_failed_shouldBuildFailureMessage() {
        // Arrange
        NotificationEvent event = NotificationEvent.builder()
                .txId("FX-UUID-002")
                .ownerId("user-200")
                .status("FAILED")
                .baseCurrency("USD")
                .targetCurrency("VND")
                .sourceAmount(new BigDecimal("1000"))
                .failureReason("Insufficient balance")
                .timestamp(Instant.now())
                .build();

        // Act
        notificationService.notify(event);

        // Assert
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationClient).send(org.mockito.ArgumentMatchers.eq("user-200"), messageCaptor.capture());

        assertThat(messageCaptor.getValue())
                .contains("thất bại")
                .contains("Insufficient balance")
                .contains("1000")
                .contains("USD");
    }

    @Test
    @DisplayName("buildMessage for SUCCESS should contain converted amount")
    void buildMessage_success_containsConvertedAmount() {
        // Arrange
        NotificationEvent event = NotificationEvent.builder()
                .status("SUCCESS")
                .baseCurrency("JPY")
                .targetCurrency("VND")
                .sourceAmount(new BigDecimal("10000"))
                .convertedAmount(new BigDecimal("1750000"))
                .build();

        // Act
        String result = notificationService.buildMessage(event);

        // Assert
        assertThat(result)
                .startsWith("Giao dịch bán")
                .contains("thành công")
                .contains("1750000")
                .contains("VND");
    }

    @Test
    @DisplayName("buildMessage for FAILED should contain failure reason")
    void buildMessage_failed_containsFailureReason() {
        // Arrange
        NotificationEvent event = NotificationEvent.builder()
                .status("FAILED")
                .baseCurrency("USD")
                .targetCurrency("VND")
                .sourceAmount(new BigDecimal("500"))
                .failureReason("Core Banking timeout")
                .build();

        // Act
        String result = notificationService.buildMessage(event);

        // Assert
        assertThat(result)
                .startsWith("Giao dịch bán")
                .contains("thất bại")
                .contains("Core Banking timeout");
    }
}
