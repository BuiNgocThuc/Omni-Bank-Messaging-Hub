package com.example.common.constant;

public class RabbitMQConstants {

    public static final String TOPIC_EXCHANGE = "x.banking.topic";

    public static final String QUEUE_EXCHANGE_PROCESS = "q.exchange.process";
    public static final String QUEUE_ACCOUNT_UPDATE = "q.account.update";
    public static final String QUEUE_AUDIT_LOG = "q.audit.log";

    public static final String ROUTING_CONVERT = "pay.convert";
    public static final String ROUTING_EXECUTE = "pay.execute";
    public static final String ROUTING_AUDIT = "pay.audit";
}
