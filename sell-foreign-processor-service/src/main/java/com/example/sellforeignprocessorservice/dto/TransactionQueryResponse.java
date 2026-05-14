package com.example.sellforeignprocessorservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionQueryResponse {

    @JsonProperty("tx_id")
    private String txId;

    @JsonProperty("transaction_status")
    private String transactionStatus;

    private DetailResponse detail;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DetailResponse {

        @JsonProperty("account_number_id")
        private String accountNumberId;

        @JsonProperty("base_currency")
        private String baseCurrency;

        @JsonProperty("target_currency")
        private String targetCurrency;

        @JsonProperty("source_amount")
        private BigDecimal sourceAmount;

        @JsonProperty("rate_exchange")
        private BigDecimal rateExchange;

        @JsonProperty("converted_amount")
        private BigDecimal convertedAmount;

        @JsonProperty("failure_reason")
        private String failureReason;

        @JsonProperty("completed_at")
        private LocalDateTime completedAt;
    }
}
