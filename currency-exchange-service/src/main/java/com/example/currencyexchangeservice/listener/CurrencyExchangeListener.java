package com.example.currencyexchangeservice.listener;

import com.example.common.constant.RabbitMQConstants;
import com.example.common.dto.message.PaymentMessage;
import com.example.common.enums.TransactionStatus;
import com.example.currencyexchangeservice.service.ICurrencyExchangeService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import com.example.common.dto.message.AuditEvent;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CurrencyExchangeListener {
    private final RabbitTemplate rabbitTemplate;
    private final ICurrencyExchangeService exchangeService;


    // khúc này nhận -> gọi API external -> đổi ngoại tệ rồi ( ko lấy phí nha)  --đã xog
    // -> gửi qua bên service ledger,audit để l
    @RabbitListener(queues = RabbitMQConstants.QUEUE_EXCHANGE_PROCESS)
    public void onMessage(PaymentMessage payload, Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        log.info("CURRENCY-SERVICE Received TX: {}", payload.getTransactionId());

        try {
            // validate + call API + tính converted
            PaymentMessage processed = exchangeService.processExchange(payload);


            // thành công -> forward sang account ledger để payment
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.TOPIC_EXCHANGE,
                    RabbitMQConstants.ROUTING_LEDGER_AND_BALANCE,
                    processed
            );
              channel.basicAck(deliveryTag, false); //ack và xóa

            log.info("CURRENCY-SERVICE Forwarded TX {} with payload {} {} {} {} {} {}",
                    processed.getTransactionId(),
                    processed.getTransactionStatus(),
                    processed.getSourceCurrency(),
                    processed.getTargetCurrency(),
                    processed.getAmount(),
                    processed.getConvertedAmount(),
                    processed.getCreatedAt());
//
        } catch (RestClientException e) {
            // Lỗi gọi API ngoài  nên requeue
            log.error("CURRENCY-SERVICE FxRatesAPI error TX {}: {} -> REQUEUE",
                    payload.getTransactionId(), e.getMessage());
            channel.basicNack(deliveryTag, false, true);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("CURRENCY-SERVICE Invalid message TX {}: {} -> DROP",
                    payload.getTransactionId(), e.getMessage());

            PaymentMessage failedMsg = PaymentMessage.builder()
                    .transactionId(payload.getTransactionId())
                    .transactionStatus(TransactionStatus.FAILED_EXCHANGE.name())
                    .failureReason(e.getMessage())
                    .createdAt(LocalDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.TOPIC_EXCHANGE,
                    RabbitMQConstants.ROUTING_TRANSACTION_UPDATE,
                    failedMsg
            );
            channel.basicNack(deliveryTag, false, false);


        } catch (Exception e) {
            log.error("CURRENCY-SERVICE Unexpected error TX {}: {} -> DROP",
                    payload.getTransactionId(), e.getMessage(), e);

            PaymentMessage failedMsg = PaymentMessage.builder()
                    .transactionId(payload.getTransactionId())
                    .transactionStatus(TransactionStatus.FAILED_EXCHANGE.name())
                    .failureReason(e.getMessage())
                    .createdAt(LocalDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.TOPIC_EXCHANGE,
                    RabbitMQConstants.ROUTING_TRANSACTION_UPDATE,
                    failedMsg
            );

            channel.basicNack(deliveryTag, false, false);

        }
    }


}
