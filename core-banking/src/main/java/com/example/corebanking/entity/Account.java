package com.example.corebanking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "Account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @Column(name = "account_number_id", updatable = false, nullable = false)
    private String accountNumberId;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "total_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalBalance;

    @Column(name = "available_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal availableBalance;

    @Column(name = "held_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal heldBalance;

    @Column(name = "status", nullable = false, length = 20)
    private String status;
}
