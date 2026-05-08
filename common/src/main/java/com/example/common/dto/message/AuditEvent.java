package com.example.common.dto.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent implements Serializable {
    private String transactionId;
    private String serviceName;          // GATEWAY / CURRENCY / LEDGER
    private String eventType;            // PAYMENT_INITIATED / EXCHANGE_PROCESSED / PAYMENT_COMPLETED
    private String status;               // PENDING / PROCESSED / COMPLETED / FAILED

    // Snapshot data
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String sourceCurrency;
    private String targetCurrency;
    private BigDecimal exchangeRate; // tí để lại rate
    private BigDecimal convertedAmount;

    private String description;          // Mô tả thêm cho event
    private String errorMessage;         // Nếu có lỗi

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime eventTime;
}
