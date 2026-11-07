package com.township.repository;

import com.township.entity.FlatLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlatLedgerEntryRepository extends JpaRepository<FlatLedgerEntry, Long> {

    List<FlatLedgerEntry> findByFlatIdOrderByEntryDateAscIdAsc(Long flatId);

    Optional<FlatLedgerEntry> findTopByFlatIdOrderByEntryDateDescIdDesc(Long flatId);
}

