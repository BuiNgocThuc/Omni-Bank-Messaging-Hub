package org.example.transactionservice.service;

import com.example.common.config.api.ApiResponse;
import com.example.common.dto.request.PaymentRequest;
import com.example.common.dto.response.TransactionData;

public interface ITransactionService {
    ApiResponse<TransactionData> initiateTransaction(PaymentRequest request);
}
