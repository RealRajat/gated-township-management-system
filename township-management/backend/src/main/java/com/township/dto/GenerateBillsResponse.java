package com.township.dto;

import java.math.BigDecimal;

public record GenerateBillsResponse(
    String billingMonth,
    int billsGeneratedCount,
    int duplicatesSkippedCount,
    BigDecimal totalAmountBilled,
    String message
) {}

