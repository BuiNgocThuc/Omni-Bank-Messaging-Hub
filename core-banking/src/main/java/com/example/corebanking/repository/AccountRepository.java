package com.example.corebanking.repository;

import com.example.common.enums.Currency;
import com.example.corebanking.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    Optional<Account> findByAccountNumberId(String accountNumberId);

    @Modifying
    @Query("""
           UPDATE Account a
           SET a.availableBalance = a.availableBalance - :amount,
               a.heldBalance = a.heldBalance + :amount
           WHERE a.accountNumberId = :accountNumberId
             AND a.currency = :currency
             AND a.availableBalance >= :amount
           """)
    int holdFundsAtomically(@Param("accountNumberId") String accountNumberId,
                            @Param("amount") BigDecimal amount,
                            @Param("currency") Currency currency);


    @Modifying
    @Query("""
           UPDATE Account a
           SET a.heldBalance = a.heldBalance - :amount,
               a.totalBalance = a.totalBalance - :amount
           WHERE a.accountNumberId = :accountNumberId
              AND a.currency = :currency
              AND a.heldBalance >= :amount
           """)
    int realeaseHold(@Param("accountNumberId") String accountNumberId,
                     @Param("amount") BigDecimal amount,
                     @Param("currency") Currency currency);


    @Modifying
    @Query("""
           UPDATE Account a
           SET a.availableBalance = a.availableBalance + :amount,
               a.totalBalance = a.totalBalance + :amount
           WHERE a.accountNumberId = :accountNumberId
              AND a.currency = :currency
           """)
    int creditAfterRelease(@Param("accountNumberId") String accountNumberId,
                     @Param("amount") BigDecimal amount,
                     @Param("currency") Currency currency);




    boolean existsByAccountNumberId(String accountNumberId);
}
