package com.example.sellforeignprocessorservice.client;

import com.example.sellforeignprocessorservice.dto.ExternalApiResponse;
import com.example.sellforeignprocessorservice.dto.TreasuryRateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "treasury-service", url = "${feign.client.treasury.url}")
public interface TreasuryClient {

    @GetMapping("/api/v1/treasury/rate")
    ExternalApiResponse<TreasuryRateResponse> getRate(
            @RequestParam("base") String base,
            @RequestParam("target") String target,
            @RequestParam("amount") String amount
    );
}
