package com.example.common.dto.message;

import com.example.common.enums.TransactionStatus;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PaymentMessage implements Serializable {
    private String transactionId;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String sourceCurrency;
    private String targetCurrency;
    private BigDecimal exchangeRate;
    private BigDecimal convertedAmount;
    private TransactionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
