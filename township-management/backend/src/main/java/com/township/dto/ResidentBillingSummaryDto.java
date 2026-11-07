package com.township.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResidentBillingSummaryDto(
    List<FlatSummaryDto> flats,
    BigDecimal currentMonthlyDues,
    BigDecimal totalPaidAmount,
    BigDecimal totalOutstandingBalance,
    long overdueCount,
    List<MaintenanceBillDto> bills
) {}

