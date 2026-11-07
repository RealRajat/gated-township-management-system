package com.township.dto;

import java.math.BigDecimal;

public record CommitteeFinancialKpiDto(
        BigDecimal totalBilled,
        BigDecimal totalCollected,
        BigDecimal totalOutstanding,
        double collectionPercentage,
        long totalBillsCount,
        long paidBillsCount,
        long partiallyPaidCount,
        BigDecimal partiallyPaidAmount,
        long unpaidCount,
        long overdueCount,
        BigDecimal overdueAmount
) {}

