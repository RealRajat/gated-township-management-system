package com.township.dto;

import java.math.BigDecimal;

public record CollectionTrendDto(
        String month,
        BigDecimal billedAmount,
        BigDecimal collectedAmount,
        BigDecimal outstandingAmount,
        double collectionRate
) {}

