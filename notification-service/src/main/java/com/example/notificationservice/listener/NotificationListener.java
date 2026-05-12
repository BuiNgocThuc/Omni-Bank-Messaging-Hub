package com.example.notificationservice.listener;

import com.example.common.constant.RabbitMQConstants;
import com.example.common.dto.message.NotificationEvent;
import com.example.notificationservice.service.NotificationService;
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
public class NotificationListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConstants.QUEUE_NOTIFICATION)
    public void onTransactionCompleted(NotificationEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {

        log.info("Received notification event for tx [{}]", event.getTxId());

        try {

            notificationService.notify(event);

            channel.basicAck(deliveryTag, false);

            log.info("Acknowledged notification event for tx [{}]", event.getTxId());

        } catch (Exception e) {

            log.error("Failed to process notification for tx [{}]: {}", event.getTxId(), e.getMessage(), e);

            try {

                channel.basicNack(deliveryTag, false, false);

            } catch (Exception nackEx) {

                log.error("Failed to nack notification message: {}", nackEx.getMessage());
            }
        }
    }
}
