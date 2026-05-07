package com.example.accountledgerservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ACCOUNT")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @Column(name = "ACCOUNT_NUMBER", length = 50)
    private String accountNumber;

    @Column(name = "OWNER_NAME", length = 100)
    private String ownerName;

    @Column(name = "BALANCE", precision = 19, scale = 4, nullable = false)
    private BigDecimal balance;

    @Column(name = "CURRENCY", length = 3, nullable = false)
    private String currency;
}