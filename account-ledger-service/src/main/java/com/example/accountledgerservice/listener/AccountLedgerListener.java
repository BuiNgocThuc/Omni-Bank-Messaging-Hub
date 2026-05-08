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

    @RabbitListener(queues = RabbitMQConstants.QUEUE_ACCOUNT_UPDATE)
    public void onMessage(PaymentMessage payload, Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        log.info("[LEDGER] Received TX: {}", payload.getTransactionId());
        try {
            iAccountLedgerService.executePayment(payload);
            channel.basicAck(deliveryTag, false);
            log.info("[LEDGER] TX {} acknowledged", payload.getTransactionId());

            sendAuditLog(payload,true);

        } catch (Exception e) {
            log.error("[LEDGER] Error TX {}: {}",
                    payload.getTransactionId(), e.getMessage(), e);
            // Happy case: drop để không loop
            channel.basicNack(deliveryTag, false, false);
            sendAuditLog(payload,false);
        }
    }

    private void sendAuditLog(PaymentMessage payload, boolean success) {
        AuditEvent auditEvent = AuditEvent
                .builder()
                .transactionId(payload.getTransactionId())
                .serviceName("Account-ledger")
                .eventType(success ? "PAYMENT_COMPLETED" : "PAYMENT_FAILED")
                .status(success? "COMPLETED":"FAILED")
                .fromAccount(payload.getFromAccount())
                .toAccount(payload.getToAccount())
                .amount(payload.getAmount())
                .sourceCurrency(payload.getSourceCurrency())
                .targetCurrency(payload.getTargetCurrency())
                //.exchangeRate(BigDecimal.ONE)
                .convertedAmount(payload.getConvertedAmount())
                .description(success ?"thanh toan thanh cong" : "that bai roi")
                .errorMessage(success ?"no" : "error roi")
                .eventTime(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConstants.FANOUT_AUDIT_EXCHANGE,
                auditEvent
        );
    }
}
