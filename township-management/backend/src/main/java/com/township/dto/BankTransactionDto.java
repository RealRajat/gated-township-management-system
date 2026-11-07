package com.township.dto;

import com.township.entity.BankMatchStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BankTransactionDto(
    Long id,
    String batchId,
    LocalDate transactionDate,
    BigDecimal amount,
    String utrReference,
    String narration,
    String upiId,
    BankMatchStatus matchStatus,
    Long matchedFlatId,
    String matchedTowerName,
    String matchedFlatNumber,
    Long matchedBillId,
    String matchedBillingMonth,
    Integer confidenceScore,
    String matchReason,
    LocalDateTime reconciledAt,
    String reconciledBy,
    Long paymentId
) {}

