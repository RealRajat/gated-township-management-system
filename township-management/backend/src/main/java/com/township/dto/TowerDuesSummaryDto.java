package com.township.dto;

import java.math.BigDecimal;

public record TowerDuesSummaryDto(
        String towerId,
        String towerName,
        long flatCount,
        BigDecimal totalBilled,
        BigDecimal totalCollected,
        BigDecimal outstandingDues,
        double collectionRate,
        long defaulterCount
) {}

