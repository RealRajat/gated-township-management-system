package com.township.repository;

import com.township.entity.BankStatementBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankStatementBatchRepository extends JpaRepository<BankStatementBatch, Long> {
    Optional<BankStatementBatch> findByBatchId(String batchId);
    List<BankStatementBatch> findAllByOrderByImportedAtDesc();
}

