package com.example.treasuryservice.service.Impl;

import com.example.common.config.api.ApiCode;
import com.example.common.enums.Currency;
import com.example.sellforeignprocessorservice.dto.TreasuryRateRequest;
import com.example.treasuryservice.client.FxRatesClient;
import com.example.treasuryservice.dto.TreasuryRateResponse;
import com.example.treasuryservice.exception.BusinessException;
import com.example.treasuryservice.service.ICurrencyExchangeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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

        BigDecimal rate;
        try {
            rate = fxRatesClient.getRate(
                    request.getBase(),
                    request.getCurrencies(),
                    1
            );
        } catch (IllegalStateException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("Rate not found")) {
                throw new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        ApiCode.FX_ERR_001,
                        "Invalid currency pair"
                );
            }
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ApiCode.TREASURY_ERR_001,
                    "Treasury unavailable"
            );
        } catch (RuntimeException ex) {
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ApiCode.TREASURY_ERR_001,
                    "Treasury unavailable"
            );
        }

        TreasuryRateResponse treasuryRateResponse = TreasuryRateResponse
                .builder()
                .txId(request.getTxId())
                .target(request.getCurrencies())
                .base(request.getBase())
                .rateExchange(rate)
                .timestamp(LocalDateTime.now())
                .build();

        log.info("TX {} | {} -> {} | rate={} | timestamp={}",
                treasuryRateResponse.getTxId(),
                treasuryRateResponse.getBase(),
                treasuryRateResponse.getTarget(),
                rate,
                treasuryRateResponse.getTimestamp());

        return treasuryRateResponse;
    }

    private void validateMessage(TreasuryRateRequest request) {
        if (request == null || request.getTxId() == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    ApiCode.MISSING_FIELD,
                    "Missing required field"
            );
        }
        if (request.getCurrencies() == null || request.getBase() == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    ApiCode.MISSING_FIELD,
                    "Missing required field"
            );
        }
        if (!Currency.isSupported(request.getBase())
                || !Currency.isSupported(request.getCurrencies())
                || request.getCurrencies().equalsIgnoreCase(request.getBase())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    ApiCode.FX_ERR_001,
                    "Invalid currency pair"
            );
        }
    }
}
