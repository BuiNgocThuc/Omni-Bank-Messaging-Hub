package com.example.paymentgatewayservice.controller;

import com.example.common.config.api.ApiResponse;
import com.example.common.dto.request.PaymentRequest;
import com.example.paymentgatewayservice.service.IPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final IPaymentService ipaymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<String>> createPayment(@Valid @RequestBody PaymentRequest request) {
        ApiResponse<String> response = ipaymentService.initiatePayment(request);
        return ResponseEntity.ok(response);
    }
}
