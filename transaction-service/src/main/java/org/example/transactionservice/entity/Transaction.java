package org.example.transactionservice.entity;

import com.example.common.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Transaction_Ne")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    private String transactionId;

    @Column(name = "FROM_ACCOUNT",nullable = false)
    private String fromAccount;

    @Column(name = "TO_ACCOUNT",nullable = false)
    private String toAccount;

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
