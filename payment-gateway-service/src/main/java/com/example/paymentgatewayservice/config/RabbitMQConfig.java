package com.example.paymentgatewayservice.config;

import com.example.common.constant.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
    public Queue exchangeProcessQueue() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_EXCHANGE_PROCESS).build();
    }

    @Bean
    public Binding bindingConvert(@Qualifier("exchangeProcessQueue")  Queue exchangeProcessQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(exchangeProcessQueue)
                .to(topicExchange)
                .with(RabbitMQConstants.ROUTING_CONVERT);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }


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
    public Binding bindingAuditFanout(@Qualifier("auditLogQueue") Queue auditLogQueue, FanoutExchange auditFanoutExchange) {
        return BindingBuilder.bind(auditLogQueue).to(auditFanoutExchange);
    }
}
