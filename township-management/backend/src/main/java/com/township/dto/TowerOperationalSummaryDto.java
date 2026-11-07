package com.township.dto;

import java.math.BigDecimal;

public record TowerOperationalSummaryDto(
        String towerId,
        String towerName,
        long visitorCount,
        long complaintCount,
        BigDecimal outstandingDues
) {}

