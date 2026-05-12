package com.example.corebanking.repository;

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
             AND a.availableBalance >= :amount
           """)
    int holdFundsAtomically(@Param("accountNumberId") String accountNumberId,
                            @Param("amount") BigDecimal amount);

    boolean existsByAccountNumberId(String accountNumberId);
}
