package com.example.accountledgerservice.service;

import com.example.common.dto.message.PaymentMessage;

public interface IAccountLedgerService {
    void executeLedgerAndUpdateBalance(PaymentMessage payload);

}
