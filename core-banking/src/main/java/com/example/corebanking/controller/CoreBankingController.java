package com.example.corebanking.controller;

import com.example.common.config.api.ApiResponse;
import com.example.corebanking.dto.HoldRequest;
import com.example.corebanking.dto.HoldResponse;
import com.example.corebanking.dto.ReleaseAndEntryRequest;
import com.example.corebanking.dto.ReleaseAndEntryResponse;
import com.example.corebanking.service.CoreBankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/core")
@RequiredArgsConstructor
@Slf4j
public class CoreBankingController {

    private final CoreBankingService coreBankingService;

    @PostMapping("/hold")
    public ResponseEntity<ApiResponse<HoldResponse>> hold(@RequestBody HoldRequest request) {
        log.info("Received request for hold for txId: {}", request.getTxId());
        HoldResponse responseData = coreBankingService.processHold(request);
        return ResponseEntity.ok(ApiResponse.success("SUCCESS", responseData));
    }

    @PostMapping("/release-and-entry")
    public ResponseEntity<ApiResponse<ReleaseAndEntryResponse>> releaseAndEntry(@RequestBody ReleaseAndEntryRequest request) {
        log.info("Received request for release-and-entry for holdId: {}", request.getHoldId());
        ReleaseAndEntryResponse responseData = coreBankingService.processReleaseAndEntry(request);
        return ResponseEntity.ok(ApiResponse.success("SUCCESS", responseData));
    }
}
