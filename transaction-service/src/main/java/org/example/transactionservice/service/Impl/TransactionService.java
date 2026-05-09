package org.example.transactionservice.service.Impl;

import com.example.common.config.api.ApiCode;
import com.example.common.config.api.ApiResponse;
import com.example.common.constant.RabbitMQConstants;
import com.example.common.dto.message.PaymentMessage;
import com.example.common.dto.request.AccountResponseDTO;
import com.example.common.dto.request.AccountsRequest;
import com.example.common.dto.request.PaymentRequest;
import com.example.common.enums.TransactionStatus;
import com.example.common.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.transactionservice.client.LedgerClient;
import org.example.transactionservice.dto.AccountValidateResponse;
import org.example.transactionservice.entity.Transaction;
import org.example.transactionservice.repository.TransactionRepository;
import org.example.transactionservice.service.ITransactionService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService implements ITransactionService {
    private final LedgerClient ledgerClient;
    private final TransactionRepository transactionRepository;
    private final RabbitTemplate rabbitTemplate;
    @Override
    @Transactional
    public ApiResponse<String> initiateTransaction(PaymentRequest request) {
        String txId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        AccountValidateResponse accountValidateResponse =  validatePaymentRequest(request);

        Transaction transaction = Transaction
                .builder()
                .transactionId(txId)
                .fromAccount(request.getFromAccount())
                .toAccount(request.getToAccount())
                .amount(request.getAmount())
                .status(TransactionStatus.PENDING)
                .build();

        transactionRepository.save(transaction);

        PaymentMessage message = PaymentMessage.builder()
                .transactionId(txId)
                .fromAccount(request.getFromAccount())
                .toAccount(request.getToAccount())
                .amount(request.getAmount())
                .sourceCurrency(accountValidateResponse.getSender().getCurrency())
                .targetCurrency(accountValidateResponse.getReceiver().getCurrency())
                .amount(request.getAmount())
                .transactionStatus(transaction.getStatus().name())
                .createdAt(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConstants.TOPIC_EXCHANGE,
                RabbitMQConstants.ROUTING_CONVERT,
                message
        );

        log.info("Published payment [{}] with key=pay.convert", txId);

        return ApiResponse.success(
                ApiCode.SUCCESS,
                "Payment is being processed - initiatePayment successfully!",
                txId,
                "/api/v1/transaction"
        );
    }

    private AccountValidateResponse validatePaymentRequest(PaymentRequest request) {
        //cung account
        if (request.getFromAccount().equalsIgnoreCase(request.getToAccount())) {
            throw new BusinessException(
                    "SAME_ACCOUNT",
                    "fromAccount and toAccount must be different");
        }


        AccountsRequest accountsRequest = AccountsRequest
                .builder()
                .accountNumbers(List.of(request.getFromAccount(), request.getToAccount()))
                .build();

        // Gọi OpenFeign sang Ledger Service
        List<AccountResponseDTO> accounts = ledgerClient.getAccounts(accountsRequest);
        log.info("Found {} accounts", accounts.size());
        log.info("Found {} sender", accounts.get(0).getAccountNumber());
        log.info("Found {} rece", accounts.get(1).getAccountNumber());
        //Tìm thông tin sender và receiver từ kết quả trả về
        AccountResponseDTO sender = accounts.stream()
                .filter(a -> a.getAccountNumber().equals(request.getFromAccount()))
                .findFirst().orElse(null);

        AccountResponseDTO receiver = accounts.stream()
                .filter(a -> a.getAccountNumber().equals(request.getToAccount()))
                .findFirst().orElse(null);

        //Kiểm tra hợp lệ
        if (sender == null) throw new IllegalArgumentException("sender is null");
        if (receiver == null) throw new IllegalArgumentException("receiver is null");

        // Check tiền (Sử dụng BigDecimal)
        if (sender.getBalance().compareTo(request.getAmount()) < 0) {
           throw  new BusinessException("ERROR_AMOUNT","so tien khong du");
        }

        log.debug("Validation passed for request: {} {} with {} amount",
                request.getFromAccount(),
                request.getToAccount(),
                request.getAmount());

    return AccountValidateResponse
            .builder()
            .sender(sender)
            .receiver(receiver)
            .build();
    }
}
