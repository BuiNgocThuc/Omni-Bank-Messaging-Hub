package com.example.auditlogservice.config;

import com.example.common.constant.RabbitMQConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    public Queue auditLogQueue() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_AUDIT_LOG).build();
    }

    @Bean
    public Binding bindingAuditAll(Queue auditLogQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(auditLogQueue)
                .to(topicExchange)
                .with(RabbitMQConstants.BINDING_AUDIT_ALL);   // "#" de đây đi tí làm
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
