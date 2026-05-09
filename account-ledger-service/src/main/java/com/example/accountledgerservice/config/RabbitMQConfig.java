package com.example.accountledgerservice.config;

import com.example.common.constant.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
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
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_LEDGER_AND_BALANCE_UPDATE).build();
    }

    @Bean
    public Binding bindingAccountUpdate(@Qualifier("accountUpdateQueue") Queue accountUpdateQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(accountUpdateQueue)
                .to(topicExchange)
                .with(RabbitMQConstants.ROUTING_LEDGER_AND_BALANCE);
    }

    @Bean
    public Queue transactionUpdateQueue() {
        return new Queue(RabbitMQConstants.QUEUE_TRANSACTION_UPDATE, true);
    }

    @Bean
    public Binding transactionUpdateBinding(@Qualifier("transactionUpdateQueue") Queue transactionUpdateQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(transactionUpdateQueue)
                .to(topicExchange)
                .with(RabbitMQConstants.ROUTING_TRANSACTION_UPDATE);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
