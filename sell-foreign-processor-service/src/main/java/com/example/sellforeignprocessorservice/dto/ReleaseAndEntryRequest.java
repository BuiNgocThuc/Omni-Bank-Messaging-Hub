package com.example.sellforeignprocessorservice.dto;

import com.example.common.enums.Currency;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReleaseAndEntryRequest {
    private String txId;
    private String holdId;
    private Currency baseCurrency;
    private Currency targetCurrency;
    private String accountNumberId;
    private BigDecimal rateExchange;
}
