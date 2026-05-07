package com.example.accountledgerservice.entity;


import com.example.common.enums.TransactionStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionHistory {
    @Id
    private String transactionId;

    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String sourceCurrency;
    private String targetCurrency;
    private BigDecimal exchangeRate;
    private BigDecimal convertedAmount;

    @Enumerated
    private TransactionStatus status;
    @CreationTimestamp
    private LocalDateTime completedAt;

}
