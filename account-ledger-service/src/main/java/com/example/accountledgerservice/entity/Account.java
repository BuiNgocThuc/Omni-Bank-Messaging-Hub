package com.example.accountledgerservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ACCOUNT")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ACCOUNT_NUMBER", length = 50)
    private String accountNumber;

    @Column(name = "OWNER_NAME", length = 100)
    private String ownerName;

    @Column(name = "BALANCE", precision = 19, scale = 4, nullable = false)
    private BigDecimal balance;

    @Column(name = "CURRENCY", length = 3, nullable = false)
    private String currency;

    @Column(name = "COUNTRY_CODE", length = 3, nullable = false)
    private String countryCode;

    @Column(name = "UPDATED_AT")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}