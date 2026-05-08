package com.example.auditlogservice.listener;

import com.example.auditlogservice.entity.AuditLog;
import com.example.auditlogservice.repository.AuditLogRepository;
import com.example.auditlogservice.service.IAuditLogService;
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
import com.example.common.dto.message.AuditEvent;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogListener {

    private final AuditLogRepository repository;
    private final IAuditLogService auditLogService;

    @RabbitListener(queues = RabbitMQConstants.QUEUE_AUDIT_LOG)
    public void onMessage(AuditEvent event, Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        log.info("[AUDIT] Received event: TX={}, service={}, type={}",
                event.getTransactionId(),
                event.getServiceName(),
                event.getEventType());
        try {
            auditLogService.recordEvent(event);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[AUDIT] Failed to record event TX={}: {}",
                    event.getTransactionId(), e.getMessage(), e);
            // Audit không nên ảnh hưởng main flow → drop
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
