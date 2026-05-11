package com.example.corebanking.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HoldResponse {
    private String holdId;
    private String txId;
    private String accountNumberId;
    private BigDecimal heldAmount;
    private String currency;
    private String holdStatus;
    private String createdAt;
}
