package com.example.sellforeignservice.service.Impl;

import com.example.common.config.api.ApiResponse;
import com.example.common.config.api.ApiCode;
import com.example.common.constant.RabbitMQConstants;
import com.example.common.dto.message.SellForeignMessage;
import com.example.common.enums.Currency;
import com.example.sellforeignservice.dto.request.SellForeignTransactionRequest;
import com.example.sellforeignservice.dto.response.SellForeignTransactionResponse;
import com.example.sellforeignservice.entity.SellForeignTransaction;
import com.example.sellforeignservice.enums.TransactionStatus;
import com.example.sellforeignservice.exception.BusinessException;
import com.example.sellforeignservice.repository.TransactionRepository;
import com.example.sellforeignservice.service.SellForeignTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellForeignTransactionServiceImpl implements SellForeignTransactionService {

    private final TransactionRepository transactionRepository;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public ApiResponse<SellForeignTransactionResponse> exchange(SellForeignTransactionRequest request) {

        validateRequest(request);

        UUID txId = UUID.randomUUID();

        // validate -> generate txId -> save transaction PROCESSING -> publish MQ
        // lưu tráanscation
        SellForeignTransaction transaction = SellForeignTransaction.builder()
                .txId(txId)
                .idempotencyKey(request.getIdempotencyKey())
                .ownerId(request.getCustomerId())
                .status(TransactionStatus.PENDING)
                .build();
        transactionRepository.save(transaction);

        SellForeignMessage message = SellForeignMessage.builder()
                .txId(txId)
                .idempotencyKey(request.getIdempotencyKey())
                .ownerId(request.getCustomerId())
                .accountNumberId(request.getAccountNumberId())
                .baseCurrency(Currency.valueOf(request.getBaseCurrency().toUpperCase()))
                .targetCurrency(Currency.valueOf(request.getTargetCurrency().toUpperCase()))
                .amount(request.getAmount())
                .timestamp(Instant.now())
                .build();

        try { // bat lỗi nếu ko gửi dc
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.TOPIC_EXCHANGE,
                    RabbitMQConstants.ROUTING_PROCESSOR,
                    message
            );
        } catch (AmqpException ex) {
            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ApiCode.MQ_ERR_001,
                    "Message queue failed"
            );
        }
        log.info("Published message [{}] to queue with routing key [{}]", txId, RabbitMQConstants.ROUTING_PROCESSOR);

        SellForeignTransactionResponse responseData = SellForeignTransactionResponse.builder()
                .txId(txId.toString())
                .message("Transaction is being processed")
                .build();

        return ApiResponse.success("PROCESSING", responseData);
    }

    private void validateRequest(SellForeignTransactionRequest request) {
        try {
            UUID.fromString(request.getIdempotencyKey());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    ApiCode.INVALID_REQUEST,
                    "Invalid request body"
            );
        }

        if (transactionRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    ApiCode.FX_ERR_003,
                    "Duplicate request"
            );
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    ApiCode.AMOUNT_NOT_POSITIVE,
                    "Amount must be greater than 0"
            );
        }
        if (request.getAmount().compareTo(new BigDecimal("1.00")) < 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    ApiCode.AMOUNT_TOO_SMALL,
                    "Minimum 1.00 currency unit"
            );
        }

        if (!Currency.isSupported(request.getBaseCurrency())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    ApiCode.FX_ERR_001,
                    "Invalid currency pair"
            );
        }
        if (!Currency.isSupported(request.getTargetCurrency())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    ApiCode.FX_ERR_001,
                    "Invalid currency pair"
            );
        }
        if (request.getBaseCurrency().equalsIgnoreCase(request.getTargetCurrency())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    ApiCode.SAME_CURRENCY,
                    "Base currency and target currency must be different"
            );
        }
    }
}
