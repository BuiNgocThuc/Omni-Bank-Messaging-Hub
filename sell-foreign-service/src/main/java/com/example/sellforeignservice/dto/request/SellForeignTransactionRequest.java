package com.example.sellforeignservice.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class SellForeignTransactionRequest {

    @NotBlank(message = "Idempotency key is required")
    @JsonProperty("idempotency_key")
    private String idempotencyKey;

    @NotBlank(message = "Owner id is required")
    @JsonProperty("owner_id")
    private String ownerId;

    @NotBlank(message = "Account number id is required")
    @JsonProperty("account_number_id")
    private String accountNumberId;

    @NotBlank(message = "Base currency is required")
    @JsonProperty("base_currency")
    private String baseCurrency;

    @NotBlank(message = "Target currency is required")
    @JsonProperty("target_currency")
    private String targetCurrency;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;


}
