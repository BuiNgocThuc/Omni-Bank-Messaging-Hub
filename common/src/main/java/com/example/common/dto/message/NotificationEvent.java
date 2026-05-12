package com.example.common.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class NotificationEvent implements Serializable {

    private String txId;
    private String ownerId;
    private String status;
    private String baseCurrency;
    private String targetCurrency;
    private BigDecimal sourceAmount;
    private BigDecimal convertedAmount;
    private String failureReason;
    private Instant timestamp;
}
