package com.example.corebanking.service.impl;

import com.example.common.exception.BusinessException;
import com.example.corebanking.dto.HoldRequest;
import com.example.corebanking.dto.HoldResponse;
import com.example.corebanking.dto.ReleaseAndEntryRequest;
import com.example.corebanking.dto.ReleaseAndEntryResponse;
import com.example.corebanking.entity.Account;
import com.example.corebanking.entity.Entry;
import com.example.corebanking.enums.EntryType;
import com.example.corebanking.repository.AccountRepository;
import com.example.corebanking.repository.EntryRepository;
import com.example.corebanking.service.CoreBankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoreBankingServiceImpl implements CoreBankingService {

    private final AccountRepository accountRepository;
    private final EntryRepository entryRepository;

    @Override
    @Transactional
    public HoldResponse processHold(HoldRequest request) {
        log.info("Processing Hold for txId: {}", request.getTxId());

        if (request.getTxId() == null || request.getTxId().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "MISSING_FIELD", "txId is required");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "AMOUNT_NOT_POSITIVE", "Amount must be positive");
        }

        Account account = accountRepository.findByAccountNumberId(request.getAccountNumberId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found"));

        if (!account.getCurrency().equalsIgnoreCase(request.getCurrency())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CURRENCY_MISMATCH", "Currency does not match account currency");
        }

        if (account.getAvailableBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_BALANCE", "Insufficient available balance");
        }

        // ── Deduct available, add to held ──
        account.setAvailableBalance(account.getAvailableBalance().subtract(request.getAmount()));
        account.setHeldBalance(account.getHeldBalance().add(request.getAmount()));
        accountRepository.save(account);

        // ── Create HOLD Entry ──
        String holdId = "HOLD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Entry holdEntry = Entry.builder()
                .entryId(holdId)
                .txId(request.getTxId())
                .accountNumberId(account.getAccountNumberId())
                .type(EntryType.HOLD)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status("ACTIVE")
                .build();
        entryRepository.save(holdEntry);

        log.info("Successfully processed Hold [{}] for txId: {}", holdId, request.getTxId());

        return HoldResponse.builder()
                .holdId(holdEntry.getEntryId())
                .txId(holdEntry.getTxId())
                .accountNumberId(holdEntry.getAccountNumberId())
                .heldAmount(holdEntry.getAmount())
                .currency(holdEntry.getCurrency())
                .holdStatus(holdEntry.getStatus())
                .createdAt(LocalDateTime.now().toString())
                .build();
    }

    @Override
    @Transactional
    public ReleaseAndEntryResponse processReleaseAndEntry(ReleaseAndEntryRequest request) {
        log.info("Processing Release and Entry for holdId: {}", request.getHoldId());

        Entry holdEntry = entryRepository.findById(request.getHoldId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "HOLD_NOT_FOUND", "Hold entry not found"));

        if (!"ACTIVE".equals(holdEntry.getStatus())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_HOLD_STATUS", "Hold is not active");
        }

        Account account = accountRepository.findByAccountNumberId(request.getAccountNumberId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found"));

        // ── Release Hold & Commit Transaction ──
        account.setHeldBalance(account.getHeldBalance().subtract(holdEntry.getAmount()));
        account.setTotalBalance(account.getTotalBalance().subtract(holdEntry.getAmount()));
        accountRepository.save(account);

        holdEntry.setStatus("COMPLETED");
        entryRepository.save(holdEntry);

        // ── Create DEBIT Entry ──
        String entryId = "ENTRY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Entry debitEntry = Entry.builder()
                .entryId(entryId)
                .txId(request.getTxId())
                .accountNumberId(account.getAccountNumberId())
                .type(EntryType.DEBIT)
                .currency(holdEntry.getCurrency())
                .amount(holdEntry.getAmount())
                .rateExchange(request.getRateExchange())
                .holdId(holdEntry.getEntryId())
                .status("COMPLETED")
                .build();
        entryRepository.save(debitEntry);

        log.info("Successfully released hold [{}] and created entry [{}] for txId: {}", request.getHoldId(), entryId, request.getTxId());

        return ReleaseAndEntryResponse.builder()
                .entryId(debitEntry.getEntryId())
                .holdId(holdEntry.getEntryId())
                .txId(debitEntry.getTxId())
                .status("SUCCESS")
                .createdAt(LocalDateTime.now().toString())
                .build();
    }
}
