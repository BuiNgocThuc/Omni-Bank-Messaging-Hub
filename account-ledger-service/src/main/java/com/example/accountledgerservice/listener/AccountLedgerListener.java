package com.example.accountledgerservice.listener;

import com.example.accountledgerservice.entity.TransactionHistory;
import com.example.accountledgerservice.repository.TransactionHistoryRepository;
import com.example.accountledgerservice.service.IAccountLedgerService;
import com.example.common.constant.RabbitMQConstants;
import com.example.common.dto.message.PaymentMessage;
import com.example.common.enums.TransactionStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.rabbitmq.client.Channel;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import com.example.common.dto.message.AuditEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountLedgerListener {

    private final IAccountLedgerService iAccountLedgerService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMQConstants.QUEUE_LEDGER_AND_BALANCE_UPDATE)
    public void onMessage(PaymentMessage payload, Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        log.info("[LEDGER] Received TX: {}", payload.getTransactionId());
        try {
            iAccountLedgerService.executeLedgerAndUpdateBalance(payload);
            log.info("[LEDGER] TX {} acknowledged", payload.getTransactionId());

            PaymentMessage successMessage = PaymentMessage.builder()
                    .transactionId(payload.getTransactionId())
                    .transactionStatus(TransactionStatus.COMPLETED_LEDGER.name())
                    .createdAt(LocalDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.TOPIC_EXCHANGE,
                    RabbitMQConstants.ROUTING_TRANSACTION_UPDATE,
                    successMessage
            );
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("[LEDGER] Error TX {}: {}",
                    payload.getTransactionId(), e.getMessage(), e);
            PaymentMessage failedMsg = PaymentMessage.builder()
                    .transactionId(payload.getTransactionId())
                    .transactionStatus(TransactionStatus.FAILED_LEDGER.name())
                    .failureReason(e.getMessage())
                    .createdAt(LocalDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.TOPIC_EXCHANGE,
                    RabbitMQConstants.ROUTING_TRANSACTION_UPDATE,
                    failedMsg
            );

            channel.basicNack(deliveryTag, false, false);
        }
    }

}
