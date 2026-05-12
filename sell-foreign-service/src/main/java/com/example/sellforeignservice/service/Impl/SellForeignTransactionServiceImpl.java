package com.example.sellforeignservice.service.Impl;

import com.example.common.config.api.ApiResponse;
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
    public ApiResponse<SellForeignTransactionResponse> exchange(SellForeignTransactionRequest request) {

        validateRequest(request);

        UUID txId = UUID.randomUUID();

        SellForeignMessage message = SellForeignMessage.builder()
                .txId(txId)
                .idempotencyKey(request.getIdempotencyKey())
                .ownerId(request.getOwnerId())
                .accountNumberId(request.getAccountNumberId())
                .baseCurrency(Currency.valueOf(request.getBaseCurrency().toUpperCase()))
                .targetCurrency(Currency.valueOf(request.getTargetCurrency().toUpperCase()))
                .amount(request.getAmount())
                .timestamp(Instant.now())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConstants.TOPIC_EXCHANGE,
                RabbitMQConstants.ROUTING_PROCESSOR,
                message
        );
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
            throw new BusinessException("INVALID_IDEMPOTENCY_KEY_FORMAT",
                    "idempotency_key must be a valid UUID");
        }

        if (transactionRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "DUPLICATE_REQUEST",
                    "This request has already been submitted");
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("AMOUNT_NOT_POSITIVE", "Amount must be greater than 0");
        }
        if (request.getAmount().compareTo(new BigDecimal("0.01")) < 0) {

            throw new BusinessException("AMOUNT_TOO_SMALL", "Minimum amount is 0.01");
        }

        if (!Currency.isSupported(request.getBaseCurrency())) {
            throw new BusinessException("UNSUPPORTED_CURRENCY_PAIR",
                    "base_currency '" + request.getBaseCurrency() + "' is not supported");
        }
        if (!Currency.isSupported(request.getTargetCurrency())) {
            throw new BusinessException("UNSUPPORTED_CURRENCY_PAIR",
                    "target_currency '" + request.getTargetCurrency() + "' is not supported");
        }
        if (request.getBaseCurrency().equalsIgnoreCase(request.getTargetCurrency())) {
            throw new BusinessException("SAME_CURRENCY",
                    "base_currency and target_currency must be different");
        }
    }
}
