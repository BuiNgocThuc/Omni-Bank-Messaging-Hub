package com.example.accountledgerservice.repository;

import com.example.accountledgerservice.entity.TransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<TransactionHistory, String> {
}
