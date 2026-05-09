package org.example.transactionservice.repository;

import org.example.transactionservice.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    Optional<Transaction> findByTransactionId(String transactionId);
}
