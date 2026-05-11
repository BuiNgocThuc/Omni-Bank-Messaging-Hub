package com.example.sellforeignservice.service;

import com.example.common.config.api.ApiResponse;
import com.example.sellforeignservice.dto.request.SellForeignTransactionRequest;
import com.example.sellforeignservice.dto.response.SellForeignTransactionResponse;

public interface SellForeignTransactionService {
    ApiResponse<SellForeignTransactionResponse>  exchange (SellForeignTransactionRequest sellForeignTransactionRequest);
}
