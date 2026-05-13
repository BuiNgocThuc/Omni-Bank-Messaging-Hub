package com.example.sellforeignprocessorservice.service;

import com.example.common.dto.message.SellForeignMessage;
import com.example.sellforeignprocessorservice.dto.TransactionalResultResponse;

import java.util.UUID;

public interface SellForeignProcessorService {

    void processTransaction(SellForeignMessage message);

    TransactionalResultResponse getTransactionResult(UUID txId);
}
