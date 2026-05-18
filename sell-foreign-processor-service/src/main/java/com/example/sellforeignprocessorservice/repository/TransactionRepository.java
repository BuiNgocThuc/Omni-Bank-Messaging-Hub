package com.example.sellforeignprocessorservice.repository;

import com.example.sellforeignprocessorservice.entity.SellForeignTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<SellForeignTransaction, UUID> {
}
