package com.example.sellforeignservice.dto.request;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SellForeignMessage implements Serializable {
    private UUID txId;
    private UUID idempotencyKey;
    private UUID ownerId;

    private Currency currencyBase;
    private Currency currencyTarget;

    private BigDecimal amount;
}
