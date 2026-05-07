package com.example.currencyexchangeservice.service;

import com.example.common.dto.message.PaymentMessage;

public interface ICurrencyExchangeService {
    PaymentMessage processExchange(PaymentMessage payload);
}
