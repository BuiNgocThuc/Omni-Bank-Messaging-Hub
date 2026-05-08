package com.example.accountledgerservice.entity;


import com.example.common.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "TRANSACTION_HISTORY", indexes = {
        @Index(name = "IDX_TX_FROM_ACC", columnList = "FROM_ACCOUNT"),
        @Index(name = "IDX_TX_TO_ACC", columnList = "TO_ACCOUNT")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionHistory {

    @Id
    @Column(name = "TRANSACTION_ID", length = 50)
    private String transactionId;

    @Column(name = "FROM_ACCOUNT", length = 50, nullable = false)
    private String fromAccount;

    @Column(name = "TO_ACCOUNT", length = 50, nullable = false)
    private String toAccount;

    @Column(name = "AMOUNT", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "SOURCE_CURRENCY", length = 3, nullable = false)
    private String sourceCurrency;

    @Column(name = "TARGET_CURRENCY", length = 3, nullable = false)
    private String targetCurrency;

    @Column(name = "CONVERTED_AMOUNT", precision = 19, scale = 4)
    private BigDecimal convertedAmount;

    @Column(name = "STATUS", length = 20, nullable = false)
    private String status;

    @Column(name = "FAILURE_REASON", length = 500)
    private String failureReason;

    @Column(name = "COMPLETED_AT", nullable = false)
    private LocalDateTime completedAt;
}
