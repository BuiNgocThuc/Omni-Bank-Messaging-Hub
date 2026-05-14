package com.example.sellforeignprocessorservice.service.impl;

import com.example.common.dto.message.NotificationEvent;
import com.example.common.dto.message.SellForeignMessage;
import com.example.sellforeignprocessorservice.client.CoreBankingClient;
import com.example.sellforeignprocessorservice.client.TreasuryClient;
import com.example.sellforeignprocessorservice.dto.*;
import com.example.sellforeignprocessorservice.entity.SellForeignTransaction;
import com.example.sellforeignprocessorservice.entity.TransactionDetail;
import com.example.sellforeignprocessorservice.enums.TransactionStatus;
import com.example.sellforeignprocessorservice.publisher.NotificationEventPublisher;
import com.example.sellforeignprocessorservice.repository.TransactionDetailRepository;
import com.example.sellforeignprocessorservice.repository.TransactionRepository;
import com.example.sellforeignprocessorservice.service.SellForeignProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellForeignProcessorServiceImpl implements SellForeignProcessorService {

    private final TransactionRepository transactionRepository;
    private final TransactionDetailRepository transactionDetailRepository;
    private final TreasuryClient treasuryClient;
    private final CoreBankingClient coreBankingClient;
    private final NotificationEventPublisher notificationEventPublisher;

    @Value("${key.throw-exception}")
    private String IDEMPO_KEY_THROW_E;

    @Override
    public void processTransaction(SellForeignMessage message) {

        SellForeignTransaction transaction = initTransaction(message);
        // Duplicate deliveries must not replay completed money movement.
        if (!(transaction.getStatus() == TransactionStatus.PROCESSING)) {
            log.info("Skipping already terminal transaction [{}] with status [{}]", message.getTxId(), transaction.getStatus());
            return;
        }
        initTransactionDetail(message);
        HoldResponse holdData = null;
        BigDecimal convertedAmount = null;

        try {

            //step1
            holdData =  holdBalance(message);

            // Get exchange rate
            TreasuryRateResponse rateData = getExchangeRate(message);

            //caculate sau khi doi dc rate
            convertedAmount = calculateConvertedAmount(message, rateData);

            //update rate exchange
            updateTransactionDetailWithRate(message.getTxId(),rateData.getRateExchange(),convertedAmount);

            //release data
            releaseHoldAndCreateEntry(message, holdData, rateData);

            // Save success
            markTransactionSuccess(transaction,message.getTxId());
            log.info("Transaction [{}] completed successfully", message.getTxId());

            publishSuccessEvent(message, convertedAmount);


        } catch (Exception e) {
            log.error("Transaction [{}] failed: {}", message.getTxId(), e.getMessage(), e);

            // If money was held, release it before marking the transaction failed.
            compensateHoldIfNeeded(message, holdData);
            markTransactionFailed(transaction, message, e);
        }
    }

    private HoldResponse holdBalance(SellForeignMessage message) {
        HoldRequest holdRequest = HoldRequest.builder()
                .txId(message.getTxId().toString())
                .accountNumberId(message.getAccountNumberId())
                .ownerId(message.getOwnerId())
                .currency(message.getBaseCurrency().name())
                .amount(message.getAmount())
                .build();

        ExternalApiResponse<HoldResponse> holdResponse = coreBankingClient.checkAndHold(holdRequest);
        HoldResponse holdData = holdResponse.getData();

        log.info("Hold success [{}] for tx [{}]", holdData.getHoldId(), message.getTxId());

        return holdData;
    }

    private TreasuryRateResponse getExchangeRate(SellForeignMessage message) {

        TreasuryRateRequest rateRequest = TreasuryRateRequest.builder()
                .txId(message.getTxId().toString())
                .base(message.getBaseCurrency().name())
                .currencies(message.getTargetCurrency().name())
                .build();

        ExternalApiResponse<TreasuryRateResponse> rateResponse = treasuryClient.getRate(rateRequest);
        TreasuryRateResponse rateData = rateResponse.getData();

        // cố tình để throw exception
        if (IDEMPO_KEY_THROW_E.equals(message.getIdempotencyKey())) {
            throw new RuntimeException("Forced exception at getExchangeRate for testing");
        }

        log.info(
                "Got rate [{}] for {}/{} timestamp: {}",
                rateData.getRateExchange(),
                rateData.getBase(),
                rateData.getTarget(),
                rateData.getTimestamp()
        );

        return rateData;
    }
    private BigDecimal calculateConvertedAmount(
            SellForeignMessage message,
            TreasuryRateResponse rateData
    ) {
        return message.getAmount().multiply(rateData.getRateExchange());
    }


    private ReleaseAndEntryResponse releaseHoldAndCreateEntry(
            SellForeignMessage message,
            HoldResponse holdData,
            TreasuryRateResponse rateData
    ) {
        ReleaseAndEntryRequest releaseRequest = ReleaseAndEntryRequest.builder()
                .txId(message.getTxId().toString())
                .holdId(holdData.getHoldId())
                .accountNumberId(message.getAccountNumberId())
                .ownerId(message.getOwnerId())
                .rateExchange(rateData.getRateExchange())
                .baseCurrency(message.getBaseCurrency())
                .targetCurrency(message.getTargetCurrency())
                .build();

        ExternalApiResponse<ReleaseAndEntryResponse> releaseResponse =
                coreBankingClient.releaseAndEntry(releaseRequest);

        ReleaseAndEntryResponse releaseData = releaseResponse.getData();

        log.info(
                "Release success [{}] entry [{}]",
                holdData.getHoldId(),
                releaseData.getEntryId()
        );

        return releaseData;
    }

    private SellForeignTransaction initTransaction(SellForeignMessage message) {
        log.info("Initializing transaction [{}]", message.getTxId());

        // The API service may have already created this transaction.
        SellForeignTransaction existingTransaction = transactionRepository.findById(message.getTxId())
                .orElse(null);
        if (existingTransaction != null) {
            existingTransaction.setStatus(TransactionStatus.PROCESSING);
            transactionRepository.save(existingTransaction);
            return existingTransaction;
        }

        SellForeignTransaction transaction = SellForeignTransaction.builder()
                .txId(message.getTxId())
                .idempotencyKey(message.getIdempotencyKey())
                .ownerId(message.getOwnerId())
                .status(TransactionStatus.PROCESSING)
                .build();

        return transactionRepository.save(transaction);
    }
    private TransactionDetail initTransactionDetail(SellForeignMessage message) {
        return transactionDetailRepository.findByTxId(message.getTxId())
                .orElseGet(() -> {
                    TransactionDetail detail = TransactionDetail.builder()
                            .txId(message.getTxId())
                            .accountNumberId(message.getAccountNumberId())
                            .baseCurrency(message.getBaseCurrency())
                            .targetCurrency(message.getTargetCurrency())
                            .sourceAmount(message.getAmount())
                            .build();

                    return transactionDetailRepository.save(detail);
                });
    }

    private void compensateHoldIfNeeded(SellForeignMessage message, HoldResponse holdData) {
        if (holdData == null) {
            return;
        }

        try {
            coreBankingClient.releaseHold(ReleaseHoldRequest.builder()
                    .txId(message.getTxId().toString())
                    .holdId(holdData.getHoldId())
                    .accountNumberId(message.getAccountNumberId())
                    .build());
            log.info("Released hold [{}] for failed tx [{}]", holdData.getHoldId(), message.getTxId());
        } catch (Exception compensationError) {
            log.error("Failed to compensate hold [{}] for tx [{}]: {}",
                    holdData.getHoldId(),
                    message.getTxId(),
                    compensationError.getMessage(),
                    compensationError);
        }
    }

    private void markTransactionFailed(
            SellForeignTransaction transaction,
            SellForeignMessage message,
            Exception e
    ) {
        TransactionDetail detail = transactionDetailRepository.findByTxId(message.getTxId())
                .orElseGet(() -> initTransactionDetail(message));

        detail.setFailureReason(e.getMessage());
        detail.setCompletedAt(LocalDateTime.now());
        transactionDetailRepository.save(detail);

        transaction.setStatus(TransactionStatus.FAILED);
        transactionRepository.save(transaction);

        publishFailedEvent(message, e);
    }





    private void markTransactionSuccess(SellForeignTransaction transaction, UUID txId) {
        transaction.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(transaction);

        transactionDetailRepository.findByTxId(txId).ifPresent(detail -> {
            detail.setCompletedAt(LocalDateTime.now());
            transactionDetailRepository.save(detail);
        });

    }

    private void updateTransactionDetailWithRate(
            UUID txId,
            BigDecimal rateExchange,
            BigDecimal convertedAmount
    ) {
        TransactionDetail detail = transactionDetailRepository.findByTxId(txId)
                .orElseThrow(() -> new IllegalStateException(
                        "Transaction detail not found for txId: " + txId
                ));

        detail.setRateExchange(rateExchange);
        detail.setConvertedAmount(convertedAmount);

        transactionDetailRepository.save(detail);
    }

    private void publishSuccessEvent(
            SellForeignMessage message,
            BigDecimal convertedAmount
    ) {
        notificationEventPublisher.publishTransactionResult(
                NotificationEvent.builder()
                        .txId(message.getTxId().toString())
                        .ownerId(message.getOwnerId())
                        .status("SUCCESS")
                        .baseCurrency(message.getBaseCurrency().name())
                        .targetCurrency(message.getTargetCurrency().name())
                        .sourceAmount(message.getAmount())
                        .convertedAmount(convertedAmount)
                        .timestamp(java.time.Instant.now())
                        .build()
        );
    }

    private void publishFailedEvent(
            SellForeignMessage message,
            Exception e
    ) {
        notificationEventPublisher.publishTransactionResult(
                NotificationEvent.builder()
                        .txId(message.getTxId().toString())
                        .ownerId(message.getOwnerId())
                        .status("FAILED")
                        .baseCurrency(message.getBaseCurrency().name())
                        .targetCurrency(message.getTargetCurrency().name())
                        .sourceAmount(message.getAmount())
                        .failureReason(e.getMessage())
                        .timestamp(java.time.Instant.now())
                        .build()
        );
    }
}
