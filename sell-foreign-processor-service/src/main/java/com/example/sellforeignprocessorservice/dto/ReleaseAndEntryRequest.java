package com.example.sellforeignprocessorservice.dto;

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
    private String accountNumberId;
    private BigDecimal rateExchange;
}
