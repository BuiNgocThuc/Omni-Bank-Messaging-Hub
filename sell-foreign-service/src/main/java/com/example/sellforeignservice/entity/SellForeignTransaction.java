package com.example.sellforeignservice.entity;

import com.example.sellforeignservice.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "Transaction_SF")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellForeignTransaction {

    @Id
    @Column(name = "tx_id", updatable = false, nullable = false)
    private UUID txId;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
