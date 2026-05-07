package com.example.paymentgatewayservice.service;

import com.example.common.config.api.ApiResponse;
import com.example.common.dto.request.PaymentRequest;

public interface IPaymentService {
    ApiResponse<String> initiatePayment(PaymentRequest request);
}
