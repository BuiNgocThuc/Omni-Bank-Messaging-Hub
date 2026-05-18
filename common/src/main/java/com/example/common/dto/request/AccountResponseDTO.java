package com.example.common.dto.request;

import lombok.*;

import java.math.BigDecimal;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AccountResponseDTO {
    private String accountNumber;
    private String ownerName;
    private BigDecimal balance;
    private String currency;
    private String countryCode;
}
