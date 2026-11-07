package com.township.repository;

import com.township.entity.Flat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlatRepository extends JpaRepository<Flat, Long> {
    Optional<Flat> findByTowerIdAndFlatNumber(String towerId, String flatNumber);
    List<Flat> findByTowerIdOrderByFlatNumberAsc(String towerId);
    boolean existsByTowerIdAndFlatNumber(String towerId, String flatNumber);
}
