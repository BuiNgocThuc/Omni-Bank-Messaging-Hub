package com.example.accountledgerservice.config;

import com.example.accountledgerservice.entity.Account;
import com.example.accountledgerservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AccountDataInitializer implements ApplicationRunner {

    private final AccountRepository accountRepository;

    @Override
    public void run(ApplicationArguments args) {

        if (accountRepository.count() > 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        List<Account> accounts = List.of(
                Account.builder()
                        .accountNumber("ACC10001")
                        .ownerName("Nguyen Van A")
                        .balance(new BigDecimal("10000.0000"))
                        .currency("USD")
                        .createdAt(now)
                        .updatedAt(now)
                        .build(),

                Account.builder()
                        .accountNumber("ACC20002")
                        .ownerName("Tran Thi B")
                        .balance(new BigDecimal("5000000.0000"))
                        .currency("VND")
                        .createdAt(now)
                        .updatedAt(now)
                        .build(),

                Account.builder()
                        .accountNumber("ACC30003")
                        .ownerName("Le Van C")
                        .balance(new BigDecimal("5000.0000"))
                        .currency("EUR")
                        .createdAt(now)
                        .updatedAt(now)
                        .build()
        );

        accountRepository.saveAll(accounts);

        System.out.println("Seeded account data");
    }
}