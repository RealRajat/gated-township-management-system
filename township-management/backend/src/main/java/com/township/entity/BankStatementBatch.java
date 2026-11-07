package com.township.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_statement_batches")
public class BankStatementBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false, unique = true, length = 100)
    private String batchId;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "total_rows", nullable = false)
    private Integer totalRows = 0;

    @Column(name = "total_credit_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalCreditAmount = BigDecimal.ZERO;

    @Column(name = "exact_matches_count", nullable = false)
    private Integer exactMatchesCount = 0;

    @Column(name = "manual_review_count", nullable = false)
    private Integer manualReviewCount = 0;

    @Column(name = "unmatched_count", nullable = false)
    private Integer unmatchedCount = 0;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "PROCESSED";

    @Column(name = "uploaded_by", length = 150)
    private String uploadedBy;

    @Column(name = "imported_at", nullable = false, updatable = false)
    private LocalDateTime importedAt = LocalDateTime.now();

    public BankStatementBatch() {}

    public BankStatementBatch(
            String batchId,
            String filename,
            Long fileSizeBytes,
            Integer totalRows,
            BigDecimal totalCreditAmount,
            Integer exactMatchesCount,
            Integer manualReviewCount,
            Integer unmatchedCount,
            String status,
            String uploadedBy) {
        this.batchId = batchId;
        this.filename = filename;
        this.fileSizeBytes = fileSizeBytes;
        this.totalRows = totalRows;
        this.totalCreditAmount = totalCreditAmount != null ? totalCreditAmount : BigDecimal.ZERO;
        this.exactMatchesCount = exactMatchesCount != null ? exactMatchesCount : 0;
        this.manualReviewCount = manualReviewCount != null ? manualReviewCount : 0;
        this.unmatchedCount = unmatchedCount != null ? unmatchedCount : 0;
        this.status = status != null ? status : "PROCESSED";
        this.uploadedBy = uploadedBy;
        this.importedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public Integer getTotalRows() { return totalRows; }
    public void setTotalRows(Integer totalRows) { this.totalRows = totalRows; }

    public BigDecimal getTotalCreditAmount() { return totalCreditAmount; }
    public void setTotalCreditAmount(BigDecimal totalCreditAmount) { this.totalCreditAmount = totalCreditAmount; }

    public Integer getExactMatchesCount() { return exactMatchesCount; }
    public void setExactMatchesCount(Integer exactMatchesCount) { this.exactMatchesCount = exactMatchesCount; }

    public Integer getManualReviewCount() { return manualReviewCount; }
    public void setManualReviewCount(Integer manualReviewCount) { this.manualReviewCount = manualReviewCount; }

    public Integer getUnmatchedCount() { return unmatchedCount; }
    public void setUnmatchedCount(Integer unmatchedCount) { this.unmatchedCount = unmatchedCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

    public LocalDateTime getImportedAt() { return importedAt; }
}

