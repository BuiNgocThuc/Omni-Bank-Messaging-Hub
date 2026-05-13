package com.example.sellforeignprocessorservice.controller;

import com.example.common.config.api.ApiResponse;
import com.example.sellforeignprocessorservice.dto.TransactionalResultResponse;
import com.example.sellforeignprocessorservice.service.SellForeignProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/processor/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionQueryController {

    private final SellForeignProcessorService processorService;

    @GetMapping("/{txId}")
    public ResponseEntity<ApiResponse<TransactionalResultResponse>> getTransactionResult(@PathVariable("txId") String txId) {

        log.info("getTransactionResult begin");

        UUID parsedTxId = UUID.fromString(txId);

        TransactionalResultResponse result = processorService.getTransactionResult(parsedTxId);

        return ResponseEntity.ok(ApiResponse.success("SUCCESS", result));
    }
}
