package com.example.corebanking.entity;

import com.example.corebanking.enums.EntryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Entry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Entry {

    @Id
    @Column(name = "entry_id", updatable = false, nullable = false)
    private String entryId;

    @Column(name = "tx_id", nullable = false)
    private String txId;

    @Column(name = "account_number_id", nullable = false)
    private String accountNumberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private EntryType type;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "rate_exchange", precision = 19, scale = 4)
    private BigDecimal rateExchange;

    @Column(name = "hold_id")
    private String holdId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
