package com.example.sellforeignprocessorservice.repository;

import com.example.sellforeignprocessorservice.entity.TransactionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionDetailRepository extends JpaRepository<TransactionDetail, UUID> {
    Optional<TransactionDetail> findByTxId(UUID txid);
}
