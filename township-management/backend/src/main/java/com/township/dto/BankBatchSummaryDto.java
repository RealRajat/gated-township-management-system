package com.township.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BankBatchSummaryDto(
    Long id,
    String batchId,
    String filename,
    Long fileSizeBytes,
    int totalRows,
    BigDecimal totalCreditAmount,
    int exactMatchesCount,
    int manualReviewCount,
    int unmatchedCount,
    int duplicatesSkippedCount,
    String status,
    String uploadedBy,
    LocalDateTime importedAt
) {
    public int totalRowsImported() {
        return totalRows;
    }
}
