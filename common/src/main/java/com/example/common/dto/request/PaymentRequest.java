package com.example.common.dto.request;

import com.example.common.enums.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotBlank(message = "fromAccount is required")
    private String fromAccount;

    @NotBlank(message = "toAccount is required")
    private String toAccount;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "sourceCurrency is required")
    private Currency sourceCurrency;

    @NotNull(message = "targetCurrency is required")
    private Currency targetCurrency;
}
