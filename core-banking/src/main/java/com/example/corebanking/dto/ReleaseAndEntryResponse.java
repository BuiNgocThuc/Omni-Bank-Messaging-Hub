package com.example.corebanking.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReleaseAndEntryResponse {
    private String entryId;
    private String holdId;
    private String txId;
    private String status;
    private String createdAt;
}
