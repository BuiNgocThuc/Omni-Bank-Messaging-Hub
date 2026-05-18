package com.example.common.dto.message;

import com.example.common.enums.Currency;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SellForeignMessage implements Serializable {
//    private UUID txId;
    private String idempotencyKey;
    private String ownerId;
    private String accountNumberId;

    private Currency baseCurrency;
    private Currency targetCurrency;

    private BigDecimal amount;
    private Instant timestamp;
}
