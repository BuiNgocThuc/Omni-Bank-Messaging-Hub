package com.example.treasuryservice.service.Impl;

import com.example.common.dto.message.PaymentMessage;
import com.example.common.enums.TransactionStatus;
import com.example.sellforeignprocessorservice.dto.TreasuryRateRequest;
import com.example.treasuryservice.client.FxRatesClient;
import com.example.treasuryservice.dto.TreasuryRateResponse;
import com.example.treasuryservice.service.ICurrencyExchangeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyExchangeServiceImpl implements ICurrencyExchangeService {
    private final FxRatesClient fxRatesClient;

    @Override
    public TreasuryRateResponse processExchange(TreasuryRateRequest request) {

        validateMessage(request);

        // Gọi API external của fxrate với 3 params lấy từ PaymentMessage
        BigDecimal rate = fxRatesClient.getRate(
                request.getBase(),    // base
                request.getCurrencies(),    // currencies (target)
                1 // là 1 để lấy tỉ giá 1:1
        );
        TreasuryRateResponse treasuryRateResponse = TreasuryRateResponse
                .builder()
                .txId(request.getTxId())
                .target(request.getCurrencies())
                .base(request.getBase())
                .rateExchange(rate)
                .timestamp(LocalDateTime.now())
                .build();

        log.info(" TX {} | {} {} → {} {}",
                treasuryRateResponse.getTxId(),
                treasuryRateResponse.getBase(), treasuryRateResponse.getTarget(),
                rate, treasuryRateResponse.getTimestamp());

        return treasuryRateResponse;
    }

    private void validateMessage(TreasuryRateRequest request) {
        if (request.getTxId() == null) {
            throw new IllegalArgumentException("transactionId is null");
        }
        if (request.getCurrencies() == null
                || request.getBase() == null) {
            throw new IllegalArgumentException("Missing currency");
        }

        if (request.getCurrencies().equalsIgnoreCase(request.getBase())) {
            throw new IllegalArgumentException("Source and target currency must be different");
        }
    }
}
