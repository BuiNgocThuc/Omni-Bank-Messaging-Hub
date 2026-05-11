package com.example.sellforeignprocessorservice.service.impl;

import com.example.common.dto.message.SellForeignMessage;
import com.example.sellforeignprocessorservice.client.CoreBankingClient;
import com.example.sellforeignprocessorservice.client.TreasuryClient;
import com.example.sellforeignprocessorservice.dto.*;
import com.example.sellforeignprocessorservice.entity.SellForeignTransaction;
import com.example.sellforeignprocessorservice.entity.TransactionDetail;
import com.example.sellforeignprocessorservice.enums.TransactionStatus;
import com.example.sellforeignprocessorservice.repository.TransactionDetailRepository;
import com.example.sellforeignprocessorservice.repository.TransactionRepository;
import com.example.sellforeignprocessorservice.service.SellForeignProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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

    @Override
    @Transactional
    public void processTransaction(SellForeignMessage message) {
        log.info("Processing transaction [{}]", message.getTxId());

        SellForeignTransaction transaction = transactionRepository.findById(message.getTxId())
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + message.getTxId()));

        try {
            // ── Step 1: Call Core Banking to check balance and HOLD ──
            HoldRequest holdRequest = HoldRequest.builder()
                    .txId(message.getTxId().toString())
                    .accountNumberId(message.getAccountNumberId())
                    .ownerId(message.getOwnerId())
                    .currency(message.getBaseCurrency().name())
                    .amount(message.getAmount())
                    .build();

            ExternalApiResponse<HoldResponse> holdResponse = coreBankingClient.hold(holdRequest);
            HoldResponse holdData = holdResponse.getData();
            log.info("Hold success [{}] for tx [{}]", holdData.getHoldId(), message.getTxId());

            // ── Step 2: Get exchange rate from Treasury ──
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
        }
    }
}
