package com.example.sellforeignservice.repository;

import com.example.sellforeignservice.entity.SellForeignTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<SellForeignTransaction, UUID> {
    boolean existsByIdempotencyKey(String idempotencyKey);
}
