package com.example.accountledgerservice.config;

import com.example.common.constant.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange topicExchange() {
        return ExchangeBuilder
                .topicExchange(RabbitMQConstants.TOPIC_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue accountUpdateQueue() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_ACCOUNT_UPDATE).build();
    }

    /**
     * Bind queue với routing key "pay.execute"
     * → Khi Service B publish key này, message vào queue của Service C
     */
    @Bean
    public Binding bindingAccountUpdate(Queue accountUpdateQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(accountUpdateQueue)
                .to(topicExchange)
                .with(RabbitMQConstants.ROUTING_EXECUTE);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
