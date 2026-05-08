package com.example.currencyexchangeservice.listener;

import com.example.common.constant.RabbitMQConstants;
import com.example.common.dto.message.PaymentMessage;
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


// forward sang account ledger để payment
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.TOPIC_EXCHANGE,
                    RabbitMQConstants.ROUTING_EXECUTE,
                    processed
            );
            sendAuditLog(payload,true,"");
       // ACK
           channel.basicAck(deliveryTag, false); // khúc này là ack để xóa quque
            log.info("CURRENCY-SERVICE Forwarded TX {} with payload {} {} {} {} {} {}",
                    processed.getTransactionId(),
                    processed.getProcessedAt(),
                    processed.getSourceCurrency(),
                    processed.getAmount(),
                    processed.getTargetCurrency(),
                    processed.getConvertedAmount(),
                    processed.getCreatedAt());
//
        } catch (RestClientException e) {
            // Lỗi gọi API ngoài  nên requeue
            log.error("CURRENCY-SERVICE FxRatesAPI error TX {}: {} -> REQUEUE",
                    payload.getTransactionId(), e.getMessage());
            channel.basicNack(deliveryTag, false, true);
            sendAuditLog(payload,false,e.getMessage());

        } catch (IllegalArgumentException | IllegalStateException e) {
            // Lỗi data -> drop luôn (requeue cũng vô ích)
            log.error("CURRENCY-SERVICE Invalid message TX {}: {} -> DROP",
                    payload.getTransactionId(), e.getMessage());
            channel.basicNack(deliveryTag, false, false);
            sendAuditLog(payload,false,e.getMessage());

        } catch (Exception e) {
            // Lỗi không biết -> DROP để tránh poison message
            log.error("CURRENCY-SERVICE Unexpected error TX {}: {} -> DROP",
                    payload.getTransactionId(), e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
            sendAuditLog(payload,false,e.getMessage());
        }
    }

    private void sendAuditLog(PaymentMessage payload, boolean success, String messageError) {
        AuditEvent auditEvent = AuditEvent
                .builder()
                .transactionId(payload.getTransactionId())
                .serviceName("currency-exchange")
                .eventType(success ? "EXCHANGE_COMPLETED" : "EXCHANGE_FAILED")
                .status(success? "COMPLETED":"FAILED")
                .fromAccount(payload.getFromAccount())
                .toAccount(payload.getToAccount())
                .amount(payload.getAmount())
                .sourceCurrency(payload.getSourceCurrency())
                .targetCurrency(payload.getTargetCurrency())
        //        .exchangeRate(BigDecimal.ONE)
                .convertedAmount(payload.getConvertedAmount())
                .description(success ?"exchange thanh cong" : "exchange that bai roi")
                .errorMessage(success ?"no" : messageError)
                .eventTime(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConstants.FANOUT_AUDIT_EXCHANGE,
                "",
                auditEvent
        );
    }

}
