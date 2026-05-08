package com.example.accountledgerservice.repository;

import com.example.accountledgerservice.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {


    @Modifying
    @Query("UPDATE Account a SET a.balance = a.balance - :amount where a.accountNumber = :accountNumber and a.balance > 0")
    int debit(@Param("accountNumber") String accountNumber,
              @Param("amount") BigDecimal amount);

    @Modifying
    @Query("UPDATE Account a SET a.balance = a.balance + :amount where a.accountNumber = :accountNumber and a.balance > 0")
    int credit(@Param("accountNumber") String accountNumber,
               @Param("amount") BigDecimal amount);
}
