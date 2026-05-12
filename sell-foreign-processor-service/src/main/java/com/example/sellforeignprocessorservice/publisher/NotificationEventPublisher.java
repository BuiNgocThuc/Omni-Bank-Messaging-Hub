package com.example.sellforeignprocessorservice.publisher;

import com.example.common.constant.RabbitMQConstants;
import com.example.common.dto.message.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishTransactionResult(NotificationEvent event) {

        rabbitTemplate.convertAndSend(RabbitMQConstants.TOPIC_EXCHANGE, RabbitMQConstants.ROUTING_NOTIFICATION, event);

        log.info("Published notification event for tx [{}] with status [{}]", event.getTxId(), event.getStatus());
    }
}
