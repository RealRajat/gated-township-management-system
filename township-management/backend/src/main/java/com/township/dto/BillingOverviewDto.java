package com.township.dto;

import java.math.BigDecimal;

public record BillingOverviewDto(
    BigDecimal totalBilledAmount,
    BigDecimal totalCollectedAmount,
    BigDecimal totalOutstandingAmount,
    double collectionRatePercentage,
    long totalBillsCount,
    long unpaidCount,
    long partiallyPaidCount,
    long paidCount,
    long overdueCount,
    BigDecimal currentBaseRate
) {}

