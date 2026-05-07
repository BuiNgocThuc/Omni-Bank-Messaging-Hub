package com.example.auditlogservice.entity;


import com.example.common.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "AUDIT_LOG", indexes = {
        @Index(name = "IDX_AUDIT_TX_ID", columnList = "TRANSACTION_ID"),
        @Index(name = "IDX_AUDIT_LOGGED_AT", columnList = "LOGGED_AT")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_seq")
    @SequenceGenerator(name = "audit_seq", sequenceName = "AUDIT_LOG_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "TRANSACTION_ID", length = 50, nullable = false)
    private String transactionId;

    @Column(name = "EVENT_STATUS", length = 20, nullable = false)
    @Enumerated
    private TransactionStatus eventStatus;          // PENDING / PROCESSED / COMPLETED / FAILED

    @Column(name = "ROUTING_KEY", length = 100)
    private String routingKey;           // pay.usd.convert / pay.usd.execute ...

    @Column(name = "FROM_ACCOUNT", length = 50)
    private String fromAccount;

    @Column(name = "TO_ACCOUNT", length = 50)
    private String toAccount;

    @Column(name = "AMOUNT", precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "SOURCE_CURRENCY", length = 3)
    private String sourceCurrency;

    @Column(name = "TARGET_CURRENCY", length = 3)
    private String targetCurrency;

    @Column(name = "EXCHANGE_RATE", precision = 19, scale = 6)
    private BigDecimal exchangeRate;

    @Column(name = "CONVERTED_AMOUNT", precision = 19, scale = 4)
    private BigDecimal convertedAmount;

    @Column(name = "MESSAGE_PAYLOAD", length = 4000)
    private String messagePayload;       // Snapshot JSON (optional)

    @Column(name = "LOGGED_AT", nullable = false)
    @CreationTimestamp
    private LocalDateTime loggedAt;
}