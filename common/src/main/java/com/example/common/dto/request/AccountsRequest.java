package com.example.common.dto.request;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountsRequest {
    private List<String> accountNumbers;
}

