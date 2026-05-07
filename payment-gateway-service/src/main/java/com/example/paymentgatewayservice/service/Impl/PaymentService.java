package com.example.paymentgatewayservice.service.Impl;

import com.example.common.config.api.ApiCode;
import com.example.common.config.api.ApiResponse;
import com.example.common.constant.RabbitMQConstants;
import com.example.common.dto.message.PaymentMessage;
import com.example.common.dto.request.PaymentRequest;
import com.example.common.enums.TransactionStatus;
import com.example.common.exception.BusinessException;
import com.example.paymentgatewayservice.service.IPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService implements IPaymentService {

    private final RabbitTemplate rabbitTemplate;

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000"); //1M thoi
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.01");


    // validate request -> khởi tạo message -> gửi qua currency service
    @Override
    public ApiResponse<String> initiatePayment(PaymentRequest request) {
        //validate request
        validatePaymentRequest(request);

        String txId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        PaymentMessage message = PaymentMessage.builder()
                .transactionId(txId)
                .fromAccount(request.getFromAccount())
                .toAccount(request.getToAccount())
                .amount(request.getAmount())
                .sourceCurrency(request.getSourceCurrency().name())
                .targetCurrency(request.getTargetCurrency().name())
                .status(TransactionStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

      try {
          rabbitTemplate.convertAndSend(
                  RabbitMQConstants.TOPIC_EXCHANGE,
                  RabbitMQConstants.ROUTING_CONVERT,
                  message
          );
      }catch (AmqpException e) {
          throw new BusinessException(
                  HttpStatus.SERVICE_UNAVAILABLE,
                  "BROKER_UNAVAILABLE",
                  "Message broker is currently unavailable, please retry later");
      }

        log.info("Published payment [{}] with key=pay.convert", txId);
        return ApiResponse.success(
                ApiCode.SUCCESS,
                "Payment is being processed - initiatePayment successfully!",
                txId,
                "/api/v1/payments"
        );
    }


    private void validatePaymentRequest(PaymentRequest request) {
        //cung account
        if (request.getFromAccount().equalsIgnoreCase(request.getToAccount())) {
            throw new BusinessException(
                    "SAME_ACCOUNT",
                    "fromAccount and toAccount must be different");
        }

        // cung loai currency
        if (request.getSourceCurrency() == request.getTargetCurrency()) {
            throw new BusinessException(
                    "SAME_CURRENCY",
                    "sourceCurrency and targetCurrency must be different");
        }

        // so tien ko hop le
        if (request.getAmount().compareTo(MIN_AMOUNT) < 0) {
            throw new BusinessException(
                    "AMOUNT_TOO_SMALL",
                    "Amount must be at least " + MIN_AMOUNT);
        }
        //so tien qua lon
        if (request.getAmount().compareTo(MAX_AMOUNT) > 0) {
            throw new BusinessException(
                    "AMOUNT_EXCEEDS_LIMIT",
                    "Amount exceeds maximum limit of " + MAX_AMOUNT);
        }

        // so thap phan hop le tối đa là 3
        if (request.getAmount().scale() > 3) {
            throw new BusinessException(
                    "AMOUNT_INVALID_SCALE",
                    "Amount must not have more than 3 decimal places");
        }

        log.debug("Validation passed for request: {} {} → {}",
                request.getAmount(),
                request.getSourceCurrency(),
                request.getTargetCurrency());
    }
}
