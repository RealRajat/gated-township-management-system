package com.township.dto;

import com.township.entity.LedgerEntryType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FlatLedgerEntryDto(
    Long id,
    Long flatId,
    String towerId,
    String towerName,
    String flatNumber,
    Long billId,
    LocalDateTime entryDate,
    LedgerEntryType entryType,
    String description,
    BigDecimal debit,
    BigDecimal credit,
    BigDecimal runningBalance,
    String transactionReference
) {}

