package com.example.treasuryservice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TreasuryRateResponse {
    private String txId;
    private String base;
    private String target;
    private BigDecimal rateExchange;
    private LocalDateTime timestamp;
}
