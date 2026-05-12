package com.example.treasuryservice.controller;

import com.example.common.config.api.ApiResponse;
import com.example.sellforeignprocessorservice.dto.ExternalApiResponse;
import com.example.sellforeignprocessorservice.dto.TreasuryRateRequest;
import com.example.treasuryservice.dto.TreasuryRateResponse;
import com.example.treasuryservice.service.Impl.CurrencyExchangeServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/treasury")
@RequiredArgsConstructor
public class TreasuryController {
    private final CurrencyExchangeServiceImpl currencyExchangeService;

    @PostMapping("/rate")
    public ResponseEntity<ApiResponse<TreasuryRateResponse>>  getRate(@RequestBody TreasuryRateRequest request) {

        TreasuryRateResponse treasuryRateResponse = currencyExchangeService.processExchange(request);
        return ResponseEntity.ok(ApiResponse.success("SUCCESS", treasuryRateResponse));
    }
}

