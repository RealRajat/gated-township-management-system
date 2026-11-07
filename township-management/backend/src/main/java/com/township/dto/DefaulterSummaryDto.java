package com.township.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DefaulterSummaryDto(
        Long flatId,
        String towerId,
        String towerName,
        String flatNumber,
        double carpetArea,
        BigDecimal outstandingDues,
        long daysOverdue,
        LocalDate oldestDueDate,
        LocalDate lastPaymentDate
) {}

