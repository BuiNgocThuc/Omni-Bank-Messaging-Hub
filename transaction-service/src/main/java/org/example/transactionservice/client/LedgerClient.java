package org.example.transactionservice.client;

import com.example.common.dto.request.AccountResponseDTO;
import com.example.common.dto.request.AccountsRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "account-ledger-service", url = "http://localhost:8083")
public interface LedgerClient {

    @PostMapping("/api/ledger/accounts")
    List<AccountResponseDTO> getAccounts(@RequestBody AccountsRequest accountsRequest);
}