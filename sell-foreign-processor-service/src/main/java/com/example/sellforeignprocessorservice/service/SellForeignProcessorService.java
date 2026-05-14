package com.example.sellforeignprocessorservice.service;

import com.example.common.dto.message.SellForeignMessage;

public interface SellForeignProcessorService {

    void processTransaction(SellForeignMessage message);
}
