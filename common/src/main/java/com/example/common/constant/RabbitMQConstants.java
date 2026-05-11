package com.example.common.constant;

public class RabbitMQConstants {

    public static final String TOPIC_EXCHANGE = "omni.banking.topic";

    public static final String QUEUE_PROCESSOR = "q.exchange.processor";

    public static final String ROUTING_PROCESSOR = "sf.processor";
    public static final String QUEUE_EXCHANGE_PROCESS = "q.exchange.process";
    public static final String QUEUE_LEDGER_AND_BALANCE_UPDATE = "q.account.update";

    public static final String QUEUE_TRANSACTION_UPDATE = "q.transaction.update";


    public static final String ROUTING_CONVERT = "pay.convert";
    public static final String ROUTING_LEDGER_AND_BALANCE = "pay.ledger";
    public static final String ROUTING_TRANSACTION_UPDATE = "pay.transaction.update";

}
