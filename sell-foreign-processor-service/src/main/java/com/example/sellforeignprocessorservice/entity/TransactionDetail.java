package com.example.sellforeignprocessorservice.entity;

import com.example.common.enums.Currency;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "Transaction_Detail_SF")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "detail_id", updatable = false, nullable = false)
    private UUID detailId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tx_id", referencedColumnName = "tx_id", nullable = false)
    private SellForeignTransaction transaction;

    @Column(name = "account_number_id", nullable = false)
    private String accountNumberId;

    @Column(name = "base_currency", nullable = false)
    @Enumerated(EnumType.STRING)
    private Currency baseCurrency;

    @Column(name = "target_currency", nullable = false)
    @Enumerated(EnumType.STRING)
    private Currency targetCurrency;

    @Column(name = "source_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal sourceAmount;

    @Column(name = "rate_exchange", precision = 19, scale = 4)
    private BigDecimal rateExchange;

    @Column(name = "converted_amount", precision = 19, scale = 2)
    private BigDecimal convertedAmount;

    @Column(name = "entry_id")
    private String entryId;

    @Column(name = "failure_reason")
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
