package com.example.accountledgerservice.service.Impl;

import com.example.accountledgerservice.entity.TransactionHistory;
import com.example.accountledgerservice.repository.AccountRepository;
import com.example.accountledgerservice.repository.TransactionHistoryRepository;
import com.example.accountledgerservice.service.IAccountLedgerService;
import com.example.common.dto.message.PaymentMessage;
import com.example.common.enums.TransactionStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountLedgerService implements IAccountLedgerService {
    private final AccountRepository accountRepository;
    private final TransactionHistoryRepository historyRepository;

    @Override
    @Transactional
    public void executePayment(PaymentMessage payload) {
        log.info("[LEDGER] Processing TX: {}", payload.getTransactionId());

        BigDecimal debitAmount = payload.getAmount();
        BigDecimal creditAmount = payload.getConvertedAmount() != null
                ? payload.getConvertedAmount()
                : payload.getAmount();

        debitFromAccount(payload.getFromAccount(), debitAmount, payload.getSourceCurrency());

        creditToAccount(payload.getToAccount(), creditAmount, payload.getTargetCurrency());

        saveHistory(payload);

        log.info("[LEDGER] TX {} COMPLETED", payload.getTransactionId());
    }


    private void debitFromAccount(String accountNumber, BigDecimal amount, String currency) {
        int rowsUpdated = accountRepository.debit(accountNumber, amount);

        if (rowsUpdated == 0) {
            throw new IllegalStateException(
                    "Failed to debit - Account not found: " + accountNumber);
        }

        log.info("[LEDGER] DEBIT {} {} from account {}",
                amount, currency, accountNumber);
    }

    private void creditToAccount(String accountNumber, BigDecimal amount, String currency) {
        int rowsUpdated = accountRepository.credit(accountNumber, amount);

        if (rowsUpdated == 0) {
            throw new IllegalStateException(
                    "Failed to credit - Account not found: " + accountNumber);
        }

        log.info("[LEDGER] CREDIT {} {} to account {}",
                amount, currency, accountNumber);
    }

    private void saveHistory(PaymentMessage payload) {
        TransactionHistory history = TransactionHistory.builder()
                .transactionId(payload.getTransactionId())
                .fromAccount(payload.getFromAccount())
                .toAccount(payload.getToAccount())
                .amount(payload.getAmount())
                .sourceCurrency(payload.getSourceCurrency())
                .targetCurrency(payload.getTargetCurrency())
                .convertedAmount(payload.getConvertedAmount())
                .status(TransactionStatus.COMPLETED.name())
                .completedAt(LocalDateTime.now())
                .build();

        historyRepository.save(history);
        log.debug("[LEDGER] History saved for TX {}", payload.getTransactionId());
    }
}
