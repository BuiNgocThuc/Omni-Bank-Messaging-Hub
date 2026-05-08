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
    public FanoutExchange auditFanoutExchange() {
        return ExchangeBuilder
                .fanoutExchange(RabbitMQConstants.FANOUT_AUDIT_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue auditLogQueue() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_AUDIT_LOG).build();
    }


    @Bean
    public Binding bindingAuditFanout(Queue auditLogQueue, FanoutExchange auditFanoutExchange) {
        return BindingBuilder.bind(auditLogQueue).to(auditFanoutExchange);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
