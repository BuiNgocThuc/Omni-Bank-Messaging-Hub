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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellForeignProcessorServiceImpl implements SellForeignProcessorService {

    private final TransactionRepository transactionRepository;
    private final TransactionDetailRepository transactionDetailRepository;
    private final TreasuryClient treasuryClient;
    private final CoreBankingClient coreBankingClient;
    private final NotificationEventPublisher notificationEventPublisher;

    @Override
    @Transactional
    public void processTransaction(SellForeignMessage message) {

        SellForeignTransaction transaction = initTransaction(message);

        try {
            // Step 1: Hold balance
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

            // Step 2: Get exchange rate
            TreasuryRateRequest rateRequest = TreasuryRateRequest.builder()
                    .txId(message.getTxId().toString())
                    .base(message.getBaseCurrency().name())
                    .currencies(message.getTargetCurrency().name())
                    .build();

            ExternalApiResponse<TreasuryRateResponse> rateResponse = treasuryClient.getRate(rateRequest);
            TreasuryRateResponse rateData = rateResponse.getData();
            log.info("Got rate [{}] for {}/{} → timestamp: {}",
                    rateData.getRateExchange(),
                    rateData.getBase(),
                    rateData.getTarget(),
                    rateData.getTimestamp());

            // Step 3: Release hold & create entry
            ReleaseAndEntryRequest releaseRequest = ReleaseAndEntryRequest.builder()
                    .txId(message.getTxId().toString())
                    .holdId(holdData.getHoldId())
                    .accountNumberId(message.getAccountNumberId())
                    .rateExchange(rateData.getRateExchange())
                    .baseCurrency(message.getBaseCurrency())
                    .targetCurrency(message.getTargetCurrency())
                    .build();

            ExternalApiResponse<ReleaseAndEntryResponse> releaseResponse = coreBankingClient.releaseAndEntry(releaseRequest);
            ReleaseAndEntryResponse releaseData = releaseResponse.getData();
            log.info("Release success [{}] → entry [{}]", holdData.getHoldId(), releaseData.getEntryId());

            // Step 4: Save success detail
            BigDecimal convertedAmount = message.getAmount().multiply(rateData.getRateExchange());

            TransactionDetail detail = TransactionDetail.builder()
                    .txId(transaction.getTxId())
                    .accountNumberId(message.getAccountNumberId())
                    .baseCurrency(message.getBaseCurrency())
                    .targetCurrency(message.getTargetCurrency())
                    .sourceAmount(message.getAmount())
                    .rateExchange(rateData.getRateExchange())
                    .convertedAmount(convertedAmount)
                    .completedAt(LocalDateTime.now())
                    .build();

            transactionDetailRepository.save(detail);

            transaction.setStatus(TransactionStatus.SUCCESS);
            transactionRepository.save(transaction);
            log.info("Transaction [{}] completed successfully", message.getTxId());

            // ── Step 6: Publish notification event ──
            notificationEventPublisher.publishTransactionResult(
                    NotificationEvent.builder()
                            .txId(message.getTxId().toString())
                            .ownerId(message.getOwnerId())
                            .status("SUCCESS")
                            .baseCurrency(message.getBaseCurrency().name())
                            .targetCurrency(message.getTargetCurrency().name())
                            .sourceAmount(message.getAmount())
                            .convertedAmount(rateData.getConvertedAmount())
                            .timestamp(java.time.Instant.now())
                            .build()
            );

        } catch (Exception e) {
            log.error("Transaction [{}] failed: {}", message.getTxId(), e.getMessage(), e);

            // Save failure detail
            TransactionDetail failDetail = TransactionDetail.builder()
                    .txId(transaction.getTxId())
                    .accountNumberId(message.getAccountNumberId())
                    .baseCurrency(message.getBaseCurrency())
                    .targetCurrency(message.getTargetCurrency())
                    .sourceAmount(message.getAmount())
                    .failureReason(e.getMessage())
                    .completedAt(LocalDateTime.now())
                    .build();

            transactionDetailRepository.save(failDetail);

            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);

            // Publish notification event for failure
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

    private SellForeignTransaction initTransaction(SellForeignMessage message) {
        log.info("Initializing transaction [{}]", message.getTxId());

        SellForeignTransaction transaction = SellForeignTransaction.builder()
                .txId(message.getTxId())
                .idempotencyKey(message.getIdempotencyKey())
                .ownerId(message.getOwnerId())
                .status(TransactionStatus.PROCESSING)
                .build();

        return transactionRepository.save(transaction);
    }
}
