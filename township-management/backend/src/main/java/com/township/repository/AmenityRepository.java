package com.township.repository;

import com.township.entity.Amenity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, Long> {

    Optional<Amenity> findByCode(String code);

    List<Amenity> findAllByIsActiveTrueOrderByNameAsc();

    List<Amenity> findAllByOrderByCreatedAtDesc();

    boolean existsByCode(String code);

    boolean existsByName(String name);

    /**
     * Pessimistic write lock on Amenity row to serialize booking evaluations and eliminate race conditions/overbooking.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Amenity a WHERE a.id = :id")
    Optional<Amenity> findByIdWithLock(@Param("id") Long id);

    long countByIsActiveTrue();
}

