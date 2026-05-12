package com.example.corebanking.service;

import com.example.corebanking.dto.HoldRequest;
import com.example.corebanking.dto.HoldResponse;
import com.example.corebanking.dto.ReleaseAndEntryRequest;
import com.example.corebanking.dto.ReleaseAndEntryResponse;

public interface CoreBankingService {
    HoldResponse processCheckAndHold(HoldRequest request);
    ReleaseAndEntryResponse processReleaseAndEntry(ReleaseAndEntryRequest request);
}
