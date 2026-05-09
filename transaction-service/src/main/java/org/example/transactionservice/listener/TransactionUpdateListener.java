package org.example.transactionservice.listener;

import com.example.common.constant.RabbitMQConstants;
import com.example.common.dto.message.PaymentMessage;
import com.example.common.enums.TransactionStatus;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.transactionservice.entity.Transaction;
import org.example.transactionservice.repository.TransactionRepository;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionUpdateListener {
    private final RabbitTemplate rabbitTemplate;
    private final TransactionRepository transactionRepository;

    @RabbitListener(queues = RabbitMQConstants.QUEUE_TRANSACTION_UPDATE)
    public void onTransactionUpdate(PaymentMessage payload, Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        log.info("[TRANSACTION] Received update TX: {}", payload.getTransactionId());

        try {
            Transaction tx = transactionRepository.findByTransactionId(payload.getTransactionId())
                    .orElseThrow(() -> new IllegalStateException("Transaction not found"));

            tx.setStatus(TransactionStatus.valueOf(payload.getTransactionStatus()));
            transactionRepository.save(tx);

            channel.basicAck(deliveryTag, false);
            log.info("[TRANSACTION] TX {} updated to {}", payload.getTransactionId(), payload.getTransactionStatus());
        } catch (Exception e) {
            log.error("[TRANSACTION] Update failed TX {}: {}", payload.getTransactionId(), e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
