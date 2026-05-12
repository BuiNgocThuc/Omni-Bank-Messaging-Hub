package com.example.sellforeignprocessorservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TreasuryRateRequest {
    private String txId;
    private String currencies;
    private String base;
}
