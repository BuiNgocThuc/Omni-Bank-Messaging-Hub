package org.example.transactionservice.controller;

import com.example.common.config.api.ApiResponse;
import com.example.common.dto.request.PaymentRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.transactionservice.service.ITransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transaction")
@RequiredArgsConstructor
public class TransactionController {

    private final ITransactionService iTransactionService;

    @PostMapping
    public ResponseEntity<ApiResponse<String>> createPayment(@Valid @RequestBody PaymentRequest request) {
        ApiResponse<String> response = iTransactionService.initiateTransaction(request);
        return ResponseEntity.ok(response);
    }


}
