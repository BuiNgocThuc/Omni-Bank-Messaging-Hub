package com.example.common.enums;

public enum TransactionStatus {
    PENDING,        // Vừa nhận request, chưa xử lý
    PROCESSING,     // Currency đang convert
    PROCESSED,      // FX xong, chờ Ledger
    COMPLETED,      // Hoàn tất, đã trừ tiền
    FAILED,         // Lỗi, KHÔNG trừ tiền
    REFUNDED,        // Đã hoàn tiền (nếu lỗi sau khi trừ)
    UNKNOWN
}
