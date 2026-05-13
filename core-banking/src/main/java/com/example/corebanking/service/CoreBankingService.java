package com.example.corebanking.service;

import com.example.corebanking.dto.HoldRequest;
import com.example.corebanking.dto.HoldResponse;
import com.example.corebanking.dto.ReleaseAndEntryRequest;
import com.example.corebanking.dto.ReleaseAndEntryResponse;
import com.example.corebanking.dto.ReleaseHoldRequest;
import com.example.corebanking.dto.ReleaseHoldResponse;

public interface CoreBankingService {
    HoldResponse processCheckAndHold(HoldRequest request);
    ReleaseAndEntryResponse processReleaseAndEntry(ReleaseAndEntryRequest request);
    ReleaseHoldResponse processReleaseHold(ReleaseHoldRequest request);
}
