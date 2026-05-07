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

@Slf4j
@Component
@RequiredArgsConstructor
public class CurrencyExchangeListener {
    private final RabbitTemplate rabbitTemplate;
    private final ICurrencyExchangeService exchangeService;


    // khúc này nhận -> gọi API external -> đổi ngoại tệ rồi ( ko lấy phí nha)  --đã xog
    // -> gửi qua bên service ledger,audit để lưu (chưa làm)

    @RabbitListener(queues = RabbitMQConstants.QUEUE_EXCHANGE_PROCESS)
    public void onMessage(PaymentMessage payload, Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        log.info("CURRENCY-SERVICE Received TX: {}", payload.getTransactionId());

        try {
            // validate + call API + tính converted
            PaymentMessage processed = exchangeService.processExchange(payload);

            //tam thời chưa forward
//            String nextRoutingKey = "pay."
//                    + processed.getSourceCurrency().toLowerCase()
//                    + ".execute";
//
//            rabbitTemplate.convertAndSend(
//                    RabbitMQConstants.TOPIC_EXCHANGE,
//                    nextRoutingKey,
//                    processed
//            );
//
//            // ACK
           channel.basicAck(deliveryTag, false); // khúc này là ack để xóa quque
            log.info("CURRENCY-SERVICE Forwarded TX {} with key={}",
                    processed.getTransactionId(), "---------"); // này log ra để check thôi
//
        } catch (RestClientException e) {
            // Lỗi gọi API ngoài  nên requeue
            log.error("CURRENCY-SERVICE FxRatesAPI error TX {}: {} -> REQUEUE",
                    payload.getTransactionId(), e.getMessage());
            channel.basicNack(deliveryTag, false, true);

        } catch (IllegalArgumentException | IllegalStateException e) {
            // Lỗi data -> drop luôn (requeue cũng vô ích)
            log.error("CURRENCY-SERVICE Invalid message TX {}: {} -> DROP",
                    payload.getTransactionId(), e.getMessage());
            channel.basicNack(deliveryTag, false, false);

        } catch (Exception e) {
            // Lỗi không biết -> DROP để tránh poison message
            log.error("CURRENCY-SERVICE Unexpected error TX {}: {} -> DROP",
                    payload.getTransactionId(), e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

}
