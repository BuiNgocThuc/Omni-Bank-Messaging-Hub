package com.example.accountledgerservice.entity;


import com.example.accountledgerservice.enums.EntryType;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "TRANSACTION_ID", length = 50)
    private String transactionId;

    @Column(name = "ACCOUNT_NUMBER", length = 50, nullable = false)
    private String accountNumber;

    @Column(name = "AMOUNT", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "ENTRY_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private EntryType entryType;

    @Column(name = "DESCRIPTION", length = 50)
    private String description;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
