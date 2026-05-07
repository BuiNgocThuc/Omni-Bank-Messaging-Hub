package com.example.accountledgerservice.listener;

import com.example.accountledgerservice.entity.TransactionHistory;
import com.example.accountledgerservice.repository.TransactionRepository;
import com.example.common.constant.RabbitMQConstants;
import com.example.common.dto.message.PaymentMessage;
import com.example.common.enums.TransactionStatus;
import com.rabbitmq.client.Channel;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountLedgerListener {
    private final TransactionRepository repository;

    @RabbitListener(queues = RabbitMQConstants.QUEUE_ACCOUNT_UPDATE)
    @Transactional
    public void onMessage(PaymentMessage payload, Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        log.info("💾 [C] Updating ledger for TX: {}", payload.getTransactionId());

        try {
            TransactionHistory history = TransactionHistory.builder()
                    .transactionId(payload.getTransactionId())
                    .fromAccount(payload.getFromAccount())
                    .toAccount(payload.getToAccount())
                    .amount(payload.getAmount())
                    .sourceCurrency(payload.getSourceCurrency())
                    .targetCurrency(payload.getTargetCurrency())
                    .exchangeRate(payload.getExchangeRate())
                    .convertedAmount(payload.getConvertedAmount())
                    .status(TransactionStatus.COMPLETED)
                    .completedAt(LocalDateTime.now())
                    .build();

            repository.save(history);

            channel.basicAck(deliveryTag, false);
            log.info("✅ [C] TX {} COMPLETED - Balance updated", payload.getTransactionId());

        } catch (Exception e) {
            log.error("❌ [C] Error TX {}: {}", payload.getTransactionId(), e.getMessage());
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
