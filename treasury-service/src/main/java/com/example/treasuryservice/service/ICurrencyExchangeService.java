package com.example.treasuryservice.service;

import com.example.common.dto.message.PaymentMessage;
import com.example.sellforeignprocessorservice.dto.TreasuryRateRequest;
import com.example.treasuryservice.dto.TreasuryRateResponse;

public interface ICurrencyExchangeService {
    TreasuryRateResponse processExchange(TreasuryRateRequest request);

}
