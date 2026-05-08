package com.example.auditlogservice.service.Impl;

import com.example.auditlogservice.entity.AuditLog;
import com.example.auditlogservice.repository.AuditLogRepository;
import com.example.auditlogservice.service.IAuditLogService;
import com.example.common.dto.message.AuditEvent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService  implements IAuditLogService {
    private final AuditLogRepository repository;

    @Override
    @Transactional
    public void recordEvent(AuditEvent event) {
        AuditLog auditLog = AuditLog.builder()
                .transactionId(event.getTransactionId())
                .serviceName(event.getServiceName())
                .eventType(event.getEventType())
                .status(event.getStatus())
                .fromAccount(event.getFromAccount())
                .toAccount(event.getToAccount())
                .amount(event.getAmount())
                .sourceCurrency(event.getSourceCurrency())
                .targetCurrency(event.getTargetCurrency())
                .exchangeRate(event.getExchangeRate())
                .convertedAmount(event.getConvertedAmount())
                .description(event.getDescription())
                .errorMessage(event.getErrorMessage())
                .eventTime(event.getEventTime() != null
                        ? event.getEventTime()
                        : LocalDateTime.now())
                .loggedAt(LocalDateTime.now())
                .build();

        repository.save(auditLog);

        log.info("📝 [AUDIT] Recorded: TX={}, service={}, type={}, status={}",
                event.getTransactionId(),
                event.getServiceName(),
                event.getEventType(),
                event.getStatus());
    }
}
