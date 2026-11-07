package com.township.repository;

import com.township.entity.BankMatchStatus;
import com.township.entity.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    Optional<BankTransaction> findByUtrReference(String utrReference);

    boolean existsByUtrReference(String utrReference);

    List<BankTransaction> findByBatchId(Long batchId);

    List<BankTransaction> findByMatchStatus(BankMatchStatus matchStatus);

    List<BankTransaction> findAllByOrderByTransactionDateDescCreatedAtDesc();

    long countByMatchStatus(BankMatchStatus matchStatus);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM BankTransaction t WHERE t.matchStatus = 'RECONCILED'")
    BigDecimal sumReconciledAmount();
}

