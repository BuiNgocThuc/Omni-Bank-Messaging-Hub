package com.example.sellforeignprocessorservice.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HoldRequest {
    private String txId;
    private String accountNumberId;
    private String ownerId;
    private String currency;
    private BigDecimal amount;
}
