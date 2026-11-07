package com.township.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentHistoryItemDto(
    Long id,
    String receiptNumber,
    String referenceNumber,
    Long billId,
    String billingMonth,
    Long flatId,
    String towerName,
    String flatNumber,
    String residentName,
    BigDecimal amount,
    LocalDateTime paymentDate,
    String paymentMethod,
    String status,
    boolean isTestMode
) {}

