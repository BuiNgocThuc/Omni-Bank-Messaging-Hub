package com.example.common.constant;

public class RabbitMQConstants {

    private RabbitMQConstants() {}

    public static final String TOPIC_EXCHANGE = "omni.banking.topic";

    public static final String QUEUE_PROCESSOR = "q.exchange.processor";

    public static final String ROUTING_PROCESSOR = "sf.processor";

    public static final String QUEUE_NOTIFICATION = "q.notification";
    public static final String ROUTING_NOTIFICATION = "notify.transaction.completed";

}
