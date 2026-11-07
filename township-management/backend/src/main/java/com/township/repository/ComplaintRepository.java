package com.township.repository;

import com.township.entity.Complaint;
import com.township.entity.ComplaintCategory;
import com.township.entity.ComplaintPriority;
import com.township.entity.ComplaintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    Optional<Complaint> findByComplaintNumber(String complaintNumber);

    boolean existsByComplaintNumber(String complaintNumber);

    List<Complaint> findByFlatIdInOrderByCreatedAtDesc(Collection<Long> flatIds);

    List<Complaint> findByResidentIdOrderByCreatedAtDesc(Long residentId);

    List<Complaint> findAllByOrderByCreatedAtDesc();

    List<Complaint> findByStatus(ComplaintStatus status);

    List<Complaint> findByPriority(ComplaintPriority priority);

    List<Complaint> findByIsCriticalSafetyEscalationTrueOrderByCreatedAtDesc();

    long countByStatus(ComplaintStatus status);

    long countByIsCriticalSafetyEscalationTrue();

    @Query("SELECT COUNT(c) FROM Complaint c WHERE c.resolutionDeadline < :now AND c.status NOT IN ('RESOLVED', 'CLOSED')")
    long countOverdueComplaints(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM Complaint c WHERE c.flat.id = :flatId AND c.category = :category AND c.status NOT IN ('RESOLVED', 'CLOSED')")
    List<Complaint> findPotentialFlatDuplicates(
            @Param("flatId") Long flatId,
            @Param("category") ComplaintCategory category);

    @Query("SELECT c FROM Complaint c WHERE c.flat.tower.id = :towerId AND c.category = :category AND c.status NOT IN ('RESOLVED', 'CLOSED')")
    List<Complaint> findPotentialTowerDuplicates(
            @Param("towerId") String towerId,
            @Param("category") ComplaintCategory category);
}

