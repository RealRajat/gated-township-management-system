package com.township.repository;

import com.township.entity.BillStatus;
import com.township.entity.MaintenanceBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceBillRepository extends JpaRepository<MaintenanceBill, Long> {

    List<MaintenanceBill> findByFlatIdOrderByBillingMonthDesc(Long flatId);

    List<MaintenanceBill> findByFlatIdInOrderByBillingMonthDesc(List<Long> flatIds);

    Optional<MaintenanceBill> findByFlatIdAndBillingMonth(Long flatId, String billingMonth);

    boolean existsByFlatIdAndBillingMonth(Long flatId, String billingMonth);

    List<MaintenanceBill> findByStatus(BillStatus status);

    List<MaintenanceBill> findAllByOrderByBillingMonthDescCreatedAtDesc();

    @Query("SELECT COALESCE(SUM(b.totalDue), 0) FROM MaintenanceBill b")
    BigDecimal sumTotalDue();

    @Query("SELECT COALESCE(SUM(b.paidAmount), 0) FROM MaintenanceBill b")
    BigDecimal sumPaidAmount();

    @Query("SELECT COALESCE(SUM(b.outstandingBalance), 0) FROM MaintenanceBill b")
    BigDecimal sumOutstandingBalance();

    long countByStatus(BillStatus status);
}

