package com.example.currencyexchangeservice.service.Impl;

import com.example.common.dto.message.PaymentMessage;
import com.example.common.enums.TransactionStatus;
import com.example.currencyexchangeservice.client.FxRatesClient;
import com.example.currencyexchangeservice.service.ICurrencyExchangeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyExchangeService implements ICurrencyExchangeService {
    private final FxRatesClient fxRatesClient;

    @Override
    public PaymentMessage processExchange(PaymentMessage payload) {

        validateMessage(payload);

        // cho delay 1s
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Gọi API external của fxrate với 3 params lấy từ PaymentMessage
        BigDecimal rate = fxRatesClient.getRate(
                payload.getSourceCurrency(),    // base
                payload.getTargetCurrency(),    // currencies (target)
                payload.getAmount()             // amount
        );

        // Update message
        payload.setConvertedAmount(rate);
        payload.setStatus(TransactionStatus.PROCESSED.name());
        payload.setProcessedAt(LocalDateTime.now());

        log.info(" TX {} | {} {} → {} {}",
                payload.getTransactionId(),
                payload.getAmount(), payload.getSourceCurrency(),
                rate, payload.getTargetCurrency());

        return payload;
    }

    private void validateMessage(PaymentMessage payload) {
        if (payload.getTransactionId() == null) {
            throw new IllegalArgumentException("transactionId is null");
        }
        if (payload.getAmount() == null
                || payload.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid amount");
        }
        if (payload.getSourceCurrency() == null
                || payload.getTargetCurrency() == null) {
            throw new IllegalArgumentException("Missing currency");
        }
        if (payload.getSourceCurrency().equalsIgnoreCase(payload.getTargetCurrency())) {
            throw new IllegalArgumentException("Source and target currency must be different");
        }
    }
}
