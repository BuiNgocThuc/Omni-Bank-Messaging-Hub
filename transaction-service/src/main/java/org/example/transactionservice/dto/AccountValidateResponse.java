package org.example.transactionservice.dto;

import com.example.common.dto.request.AccountResponseDTO;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountValidateResponse {
    AccountResponseDTO sender;
    AccountResponseDTO receiver;
}
