package com.example.sellforeignprocessorservice.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TreasuryRateResponse {
    private String base;
    private String target;
    private BigDecimal rateExchange;
    private BigDecimal amount;
    private BigDecimal convertedAmount;
    private String timestamp;
}
