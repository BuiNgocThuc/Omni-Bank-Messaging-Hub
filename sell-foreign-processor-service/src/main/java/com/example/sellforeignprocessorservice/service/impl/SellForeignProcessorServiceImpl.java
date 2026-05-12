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

        initTransaction(message);


        try {
            // call core banking để hold và check cái balance (chưa litmit)
            HoldRequest holdRequest = HoldRequest.builder()
                    .txId(message.getTxId().toString())
                    .accountNumberId(message.getAccountNumberId())
                    .ownerId(message.getOwnerId())
                    .currency(message.getBaseCurrency().name())
                    .amount(message.getAmount())
                    .build();
            //1.check balance and hold
            ExternalApiResponse<HoldResponse> holdResponse = coreBankingClient.checkAndHold(holdRequest);
            HoldResponse holdData = holdResponse.getData();
            log.info("Hold success [{}] for tx [{}]", holdData.getHoldId(), message.getTxId());





            // 2. sau khi ok rồi h đi đổi tỉ giá
            ExternalApiResponse<TreasuryRateResponse> rateResponse = treasuryClient.getRate(
                    message.getBaseCurrency().name(),
                    message.getTargetCurrency().name(),
                    message.getAmount().toPlainString()
            );

            TreasuryRateResponse rateData = rateResponse.getData();
            log.info("Got rate [{}] for {}/{} — converted: {}",
                    rateData.getRateExchange(),
                    rateData.getBase(),
                    rateData.getTarget(),
                    rateData.getConvertedAmount());

            // ── Step 3: Call Core Banking to Release Hold and Create Entry ──
            ReleaseAndEntryRequest releaseRequest = ReleaseAndEntryRequest.builder()
                    .txId(message.getTxId().toString())
                    .holdId(holdData.getHoldId())
                    .accountNumberId(message.getAccountNumberId())
                    .rateExchange(rateData.getRateExchange())
                    .build();

            ExternalApiResponse<ReleaseAndEntryResponse> releaseResponse = coreBankingClient.releaseAndEntry(releaseRequest);
            ReleaseAndEntryResponse releaseData = releaseResponse.getData();
            log.info("Release success [{}] — entry [{}]", holdData.getHoldId(), releaseData.getEntryId());

            // ── Step 4: Save transaction detail ──
            TransactionDetail detail = TransactionDetail.builder()
                    .txId(message.getTxId())
                    .accountNumberId(message.getAccountNumberId())
                    .baseCurrency(message.getBaseCurrency().name())
                    .targetCurrency(message.getTargetCurrency().name())
                    .sourceAmount(message.getAmount())
                    .rateExchange(rateData.getRateExchange())
                    .convertedAmount(rateData.getConvertedAmount())
                    .holdId(holdData.getHoldId())
                    .entryId(releaseData.getEntryId())
                    .completedAt(LocalDateTime.now())
                    .build();

            transactionDetailRepository.save(detail);

            // ── Step 5: Update transaction status → SUCCESS ──
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

            // Save detail with failure reason
            TransactionDetail failDetail = TransactionDetail.builder()
                    .txId(message.getTxId())
                    .accountNumberId(message.getAccountNumberId())
                    .baseCurrency(message.getBaseCurrency().name())
                    .targetCurrency(message.getTargetCurrency().name())
                    .sourceAmount(message.getAmount())
                    .failureReason(e.getMessage())
                    .completedAt(LocalDateTime.now())
                    .build();

            transactionDetailRepository.save(failDetail);

            // Update transaction status → FAILED
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void initTransaction(SellForeignMessage message) {
        log.info("Processing transaction [{}]", message.getTxId());

        SellForeignTransaction transaction = SellForeignTransaction.builder()
                .txId(message.getTxId())
                .idempotencyKey(message.getIdempotencyKey())
                .ownerId(message.getOwnerId())
                .status(TransactionStatus.PROCESSING)
                .build();

        TransactionDetail detail = TransactionDetail.builder()
                .accountNumberId(message.getAccountNumberId())
                .baseCurrency(message.getBaseCurrency() != null ? message.getBaseCurrency() : null)
                .targetCurrency(message.getTargetCurrency() != null ? message.getTargetCurrency() : null)
                .sourceAmount(message.getAmount())
                .transaction(transaction)
                .build();

        transaction.setDetail(detail);

        transactionRepository.save(transaction);
    }

}
