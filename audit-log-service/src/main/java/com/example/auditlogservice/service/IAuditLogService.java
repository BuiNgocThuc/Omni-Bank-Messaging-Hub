package com.example.auditlogservice.service;
import com.example.common.dto.message.AuditEvent;

public interface IAuditLogService {
    void recordEvent(AuditEvent event);
}