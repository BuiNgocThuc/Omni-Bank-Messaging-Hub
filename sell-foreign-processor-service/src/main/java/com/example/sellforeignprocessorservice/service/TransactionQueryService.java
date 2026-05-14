package com.example.sellforeignprocessorservice.service;

import com.example.sellforeignprocessorservice.dto.TransactionQueryResponse;

import java.util.UUID;

public interface TransactionQueryService {
    TransactionQueryResponse getTransactionResult(UUID txId);
}
