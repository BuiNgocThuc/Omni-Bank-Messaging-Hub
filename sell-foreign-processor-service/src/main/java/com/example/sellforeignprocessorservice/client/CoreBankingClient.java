package com.example.sellforeignprocessorservice.client;

import com.example.sellforeignprocessorservice.dto.ExternalApiResponse;
import com.example.sellforeignprocessorservice.dto.HoldRequest;
import com.example.sellforeignprocessorservice.dto.HoldResponse;
import com.example.sellforeignprocessorservice.dto.ReleaseAndEntryRequest;
import com.example.sellforeignprocessorservice.dto.ReleaseAndEntryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "core-banking-service", url = "${feign.client.core-banking.url}")
public interface CoreBankingClient {

    @PostMapping("/api/v1/core/hold")
    ExternalApiResponse<HoldResponse> hold(@RequestBody HoldRequest request);

    @PostMapping("/api/v1/core/release-and-entry")
    ExternalApiResponse<ReleaseAndEntryResponse> releaseAndEntry(@RequestBody ReleaseAndEntryRequest request);
}
