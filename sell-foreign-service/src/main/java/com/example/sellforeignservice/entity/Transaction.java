package com.example.sellforeignservice.entity;

import com.example.common.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "Transaction_SF")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @Column(name = "tx_id", updatable = false, nullable = false)
    private UUID tx_id;

    @Column(name = "request_id",nullable = false,unique = true)
    private String request_id;

    @Column(name = "owner_id",nullable = false)
    private UUID owner_id;

    @Column(name = "AMOUNT",nullable = false)
    private BigDecimal amount;


    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS",nullable = false)
    private TransactionStatus status;

    @CreationTimestamp
    @Column(name = "CREATED_AT)",nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT",nullable = false)
    private LocalDateTime updatedAt;
}
