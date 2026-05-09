package com.example.accountledgerservice.controller;

import com.example.accountledgerservice.entity.Account;
import com.example.accountledgerservice.repository.AccountRepository;
import com.example.common.dto.request.AccountResponseDTO;
import com.example.common.dto.request.AccountsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
public class AccountController {
    private final AccountRepository accountRepository;

    @PostMapping("/accounts")
    public List<AccountResponseDTO> getAccounts(@RequestBody AccountsRequest accountsRequest) {
        List<Account> accounts = accountRepository.findAllByAccountNumberIn(accountsRequest.getAccountNumbers());

        return accounts.stream().map(acc -> AccountResponseDTO.builder()
                .accountNumber(acc.getAccountNumber())
                .balance(acc.getBalance())
                .currency(acc.getCurrency())
                .ownerName(acc.getOwnerName())
                .countryCode(acc.getCountryCode())
                .build()
        ).collect(Collectors.toList());
    }
}
