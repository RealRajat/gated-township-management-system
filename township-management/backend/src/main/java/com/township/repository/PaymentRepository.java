package com.township.repository;

import com.township.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByBillIdOrderByPaymentDateDesc(Long billId);

    List<Payment> findByFlatIdOrderByPaymentDateDesc(Long flatId);

    List<Payment> findByFlatIdInOrderByPaymentDateDesc(List<Long> flatIds);

    Optional<Payment> findByReferenceNumber(String referenceNumber);

    boolean existsByReferenceNumber(String referenceNumber);

    List<Payment> findAllByOrderByPaymentDateDesc();
}
