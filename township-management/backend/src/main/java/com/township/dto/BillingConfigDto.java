package com.township.dto;

import java.math.BigDecimal;

public record BillingConfigDto(
    Long id,
    BigDecimal baseRatePerSqFt,
    Integer gracePeriodDays,
    String lateFeeType,
    BigDecimal lateFeePercentage,
    BigDecimal lateFeeFixedAmount,
    BigDecimal sinkingFundPerFlat,
    boolean active
) {}

