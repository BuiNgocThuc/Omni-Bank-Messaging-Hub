package org.example.transactionservice.service;

import com.example.common.config.api.ApiResponse;
import com.example.common.dto.request.PaymentRequest;

public interface ITransactionService {
    ApiResponse<String> initiateTransaction(PaymentRequest request);;
}
