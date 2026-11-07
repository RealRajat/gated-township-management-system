package com.township.dto;

import com.township.entity.BillStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MaintenanceBillDto(
    Long id,
    Long flatId,
    String towerId,
    String towerName,
    String flatNumber,
    String billingMonth,
    BigDecimal flatAreaSqFt,
    BigDecimal ratePerSqFt,
    BigDecimal baseAmount,
    BigDecimal lateFee,
    BigDecimal totalDue,
    BigDecimal paidAmount,
    BigDecimal outstandingBalance,
    BillStatus status,
    LocalDate issueDate,
    LocalDate dueDate,
    String notes
) {}

