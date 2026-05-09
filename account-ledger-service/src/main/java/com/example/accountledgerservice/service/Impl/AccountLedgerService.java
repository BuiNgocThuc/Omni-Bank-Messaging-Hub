package com.example.accountledgerservice.service.Impl;

import com.example.accountledgerservice.entity.TransactionHistory;
import com.example.accountledgerservice.enums.EntryType;
import com.example.accountledgerservice.repository.AccountRepository;
import com.example.accountledgerservice.repository.TransactionHistoryRepository;
import com.example.accountledgerservice.service.IAccountLedgerService;
import com.example.common.dto.message.PaymentMessage;
import com.example.common.enums.TransactionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public void executeLedgerAndUpdateBalance(PaymentMessage payload) {
        log.info("[LEDGER] Processing TX: {}", payload.getTransactionId());
        if(!payload.getTransactionStatus().equals(TransactionStatus.EXCHANGED.name())){
            throw new IllegalArgumentException("Invalid status payload - must be EXCHANGED");
        }
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
        TransactionHistory debitEntry = TransactionHistory.builder()
                .transactionId(payload.getTransactionId())
                .accountNumber(payload.getFromAccount())
                .amount(payload.getAmount().negate())
                .entryType(EntryType.DEBIT)
                .description("Transfer to " + payload.getToAccount())
                .build();

        BigDecimal creditAmount = payload.getConvertedAmount() != null
                ? payload.getConvertedAmount()
                : payload.getAmount();

        TransactionHistory creditEntry = TransactionHistory.builder()
                .transactionId(payload.getTransactionId())
                .accountNumber(payload.getToAccount())
                .amount(creditAmount)
                .entryType(EntryType.CREDIT)
                .description("Transfer from " + payload.getFromAccount())
                .build();

        historyRepository.save(debitEntry);
        historyRepository.save(creditEntry);

        log.debug("[LEDGER] History saved for TX {}", payload.getTransactionId());
    }
}
