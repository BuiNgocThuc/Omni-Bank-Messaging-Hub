package com.example.auditlogservice.listener;

import com.example.auditlogservice.entity.AuditLog;
import com.example.auditlogservice.repository.AuditLogRepository;
import com.example.common.constant.RabbitMQConstants;
import com.example.common.dto.message.PaymentMessage;
import com.example.common.enums.TransactionStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogListener {

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConstants.QUEUE_AUDIT_LOG)
    public void onMessage(PaymentMessage payload, Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();

        log.info("[AUDIT-SERVICE] Capturing event - TX={}, Status={}, RoutingKey={}",
                payload.getTransactionId(), payload.getStatus(), routingKey);

        try {
            String snapshot = safeSerialize(payload);

            AuditLog auditLog = AuditLog.builder()
                    .transactionId(payload.getTransactionId())
                    .eventStatus(payload.getStatus() != null ? payload.getStatus() : TransactionStatus.UNKNOWN)
                    .routingKey(routingKey)
                    .fromAccount(payload.getFromAccount())
                    .toAccount(payload.getToAccount())
                    .amount(payload.getAmount())
                    .sourceCurrency(payload.getSourceCurrency())
                    .targetCurrency(payload.getTargetCurrency())
                    .exchangeRate(payload.getExchangeRate())
                    .convertedAmount(payload.getConvertedAmount())
                    .messagePayload(snapshot)
                    .loggedAt(LocalDateTime.now())
                    .build();

            repository.save(auditLog);

            channel.basicAck(deliveryTag, false);
            log.info("[AUDIT-SERVICE]  Logged TX {} successfully", payload.getTransactionId());

        } catch (Exception e) {
            log.error("[AUDIT-SERVICE]  Failed to log TX {}: {}",
                    payload.getTransactionId(), e.getMessage());
            channel.basicNack(deliveryTag, false, true);   // Requeue
        }
    }

    private String safeSerialize(PaymentMessage msg) {
        try {
            String json = objectMapper.writeValueAsString(msg);
            return json.length() > 4000 ? json.substring(0, 4000) : json;
        } catch (Exception e) {
            return "SERIALIZATION_ERROR: " + e.getMessage();
        }
    }
}
