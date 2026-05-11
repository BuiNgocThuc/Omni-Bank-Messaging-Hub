package com.example.sellforeignprocessorservice.listener;

import com.example.common.constant.RabbitMQConstants;
import com.example.common.dto.message.SellForeignMessage;
import com.example.sellforeignprocessorservice.service.SellForeignProcessorService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SellForeignMessageListener {

    private final SellForeignProcessorService processorService;

    @RabbitListener(queues = RabbitMQConstants.QUEUE_PROCESSOR)
    public void onMessage(SellForeignMessage message,
                          Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("Received message for tx [{}]", message.getTxId());

        try {
            processorService.processTransaction(message);
            channel.basicAck(deliveryTag, false);
            log.info("Acknowledged message for tx [{}]", message.getTxId());

        } catch (Exception e) {
            log.error("Failed to process tx [{}]: {}", message.getTxId(), e.getMessage(), e);
            try {
                // requeue = false → send to DLQ if configured, otherwise discard
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception nackEx) {
                log.error("Failed to nack message: {}", nackEx.getMessage());
            }
        }
    }
}
