package com.example.corebanking.repository;

import com.example.corebanking.entity.Entry;
import com.example.corebanking.enums.EntryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EntryRepository extends JpaRepository<Entry, String> {
    boolean existsByTxId(String txId);
    Optional<Entry> findByTxIdAndType(String txId, EntryType type);


}
