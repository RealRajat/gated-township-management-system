package com.township.repository;

import com.township.entity.Visitor;
import com.township.entity.VisitorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VisitorRepository extends JpaRepository<Visitor, Long> {

    Optional<Visitor> findByPassCode(String passCode);

    Optional<Visitor> findByPassToken(String passToken);

    Optional<Visitor> findByOfflineSyncId(String offlineSyncId);

    List<Visitor> findByFlatIdInOrderByCreatedAtDesc(List<Long> flatIds);

    List<Visitor> findByFlatIdInAndStatusOrderByCreatedAtDesc(List<Long> flatIds, VisitorStatus status);

    List<Visitor> findByStatusOrderByCreatedAtDesc(VisitorStatus status);

    List<Visitor> findByStatusInAndValidUntilAfterOrderByValidFromAsc(List<VisitorStatus> statuses, LocalDateTime now);

    @Query("""
        SELECT v FROM Visitor v
        WHERE LOWER(v.passCode) = LOWER(TRIM(:lookup))
           OR v.passToken = TRIM(:lookup)
           OR v.visitorPhone = TRIM(:lookup)
           OR v.visitorPhone = CONCAT('+', TRIM(:lookup))
           OR REPLACE(v.visitorPhone, '+', '') = REPLACE(TRIM(:lookup), '+', '')
    """)
    List<Visitor> lookupByPassCodeOrPhoneOrToken(@Param("lookup") String lookup);

    @Query("SELECT v FROM Visitor v WHERE v.photoStoragePath IS NOT NULL AND v.photoCapturedAt <= :cutoff AND v.photoDeletedAt IS NULL")
    List<Visitor> findExpiredPhotosForPurge(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT COUNT(v) FROM Visitor v WHERE v.status = :status")
    long countByStatus(@Param("status") VisitorStatus status);

    @Query("SELECT COUNT(v) FROM Visitor v WHERE v.checkInTime >= :startOfDay")
    long countTodayCheckIns(@Param("startOfDay") LocalDateTime startOfDay);

    @Query("""
        SELECT v FROM Visitor v
        WHERE (:status IS NULL OR v.status = :status)
          AND (:flatId IS NULL OR v.flat.id = :flatId)
          AND (:towerId IS NULL OR v.flat.tower.id = :towerId)
        ORDER BY v.createdAt DESC
    """)
    List<Visitor> findWithFilters(
            @Param("status") VisitorStatus status,
            @Param("flatId") Long flatId,
            @Param("towerId") String towerId);
}
