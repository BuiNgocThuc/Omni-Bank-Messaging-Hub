package com.example.notificationservice.client;

import io.netty.handler.ssl.SslClientHelloHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class InAppNotificationClientTest {

    private final InAppNotificationClient client = new InAppNotificationClient();

    @Test
    @DisplayName("send should not throw exception (smoke test)")
    void send_shouldNotThrow() {
        // Act & Assert
        assertThatCode(() -> client.send("user-001", "Test notification message")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("send with null userId should not throw")
    void send_nullUserId_shouldNotThrow() {
        assertThatCode(() -> client.send(null, "Message for null user"))
                .doesNotThrowAnyException();
    }
}
