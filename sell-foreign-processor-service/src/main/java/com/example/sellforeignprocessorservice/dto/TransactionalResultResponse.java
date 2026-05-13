package com.example.sellforeignprocessorservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionalResultResponse {

    private String txId;
    private String transactionStatus;
    private DetailResponse detail;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DetailResponse {
        private String accountNumberId;
        private String baseCurrency;
        private String targetCurrency;
        private BigDecimal sourceAmount;
        private BigDecimal rateExchange;
        private BigDecimal convertedAmount;
        private String failureReason;
        private LocalDateTime completedAt;
    }
}
