package com.example.sellforeignprocessorservice.service.impl;

import com.example.common.exception.BusinessException;
import com.example.sellforeignprocessorservice.dto.TransactionQueryResponse;
import com.example.sellforeignprocessorservice.entity.SellForeignTransaction;
import com.example.sellforeignprocessorservice.repository.TransactionDetailRepository;
import com.example.sellforeignprocessorservice.repository.TransactionRepository;
import com.example.sellforeignprocessorservice.service.TransactionQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionQueryServiceImpl implements TransactionQueryService {

    private final TransactionRepository transactionRepository;
    private final TransactionDetailRepository transactionDetailRepository;

    @Override
    public TransactionQueryResponse getTransactionResult(UUID txId) {

        SellForeignTransaction transaction = transactionRepository.findById(txId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "TX_NOT_FOUND", "Transaction not found: " + txId));

        TransactionQueryResponse.DetailResponse detailResponse = transactionDetailRepository.findByTxId(txId)
                .map(detail -> TransactionQueryResponse.DetailResponse.builder()
                        .accountNumberId(detail.getAccountNumberId())
                        .baseCurrency(detail.getBaseCurrency().name())
                        .targetCurrency(detail.getTargetCurrency().name())
                        .sourceAmount(detail.getSourceAmount())
                        .rateExchange(detail.getRateExchange())
                        .convertedAmount(detail.getConvertedAmount())
                        .failureReason(detail.getFailureReason())
                        .completedAt(detail.getCompletedAt())
                        .build())
                .orElse(null);

        return TransactionQueryResponse.builder()
                .txId(transaction.getTxId().toString())
                .transactionStatus(transaction.getStatus().name())
                .detail(detailResponse)
                .build();
    }
}
