package com.township.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_transactions")
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private BankStatementBatch batch;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "utr_reference", nullable = false, unique = true, length = 100)
    private String utrReference;

    @Column(name = "narration", nullable = false, length = 500)
    private String narration;

    @Column(name = "upi_id", length = 150)
    private String upiId;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false, length = 40)
    private BankMatchStatus matchStatus = BankMatchStatus.UNMATCHED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_flat_id")
    private Flat matchedFlat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_bill_id")
    private MaintenanceBill matchedBill;

    @Column(name = "confidence_score", nullable = false)
    private Integer confidenceScore = 0;

    @Column(name = "match_reason")
    private String matchReason;

    @Column(name = "reconciled_at")
    private LocalDateTime reconciledAt;

    @Column(name = "reconciled_by", length = 150)
    private String reconciledBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public BankTransaction() {}

    public BankTransaction(
            BankStatementBatch batch,
            LocalDate transactionDate,
            BigDecimal amount,
            String utrReference,
            String narration,
            String upiId,
            BankMatchStatus matchStatus,
            Flat matchedFlat,
            MaintenanceBill matchedBill,
            Integer confidenceScore,
            String matchReason) {
        this.batch = batch;
        this.transactionDate = transactionDate;
        this.amount = amount;
        this.utrReference = utrReference;
        this.narration = narration;
        this.upiId = upiId;
        this.matchStatus = matchStatus != null ? matchStatus : BankMatchStatus.UNMATCHED;
        this.matchedFlat = matchedFlat;
        this.matchedBill = matchedBill;
        this.confidenceScore = confidenceScore != null ? confidenceScore : 0;
        this.matchReason = matchReason;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BankStatementBatch getBatch() { return batch; }
    public void setBatch(BankStatementBatch batch) { this.batch = batch; }

    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getUtrReference() { return utrReference; }
    public void setUtrReference(String utrReference) { this.utrReference = utrReference; }

    public String getNarration() { return narration; }
    public void setNarration(String narration) { this.narration = narration; }

    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }

    public BankMatchStatus getMatchStatus() { return matchStatus; }
    public void setMatchStatus(BankMatchStatus matchStatus) { this.matchStatus = matchStatus; }

    public Flat getMatchedFlat() { return matchedFlat; }
    public void setMatchedFlat(Flat matchedFlat) { this.matchedFlat = matchedFlat; }

    public MaintenanceBill getMatchedBill() { return matchedBill; }
    public void setMatchedBill(MaintenanceBill matchedBill) { this.matchedBill = matchedBill; }

    public Integer getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Integer confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getMatchReason() { return matchReason; }
    public void setMatchReason(String matchReason) { this.matchReason = matchReason; }

    public LocalDateTime getReconciledAt() { return reconciledAt; }
    public void setReconciledAt(LocalDateTime reconciledAt) { this.reconciledAt = reconciledAt; }

    public String getReconciledBy() { return reconciledBy; }
    public void setReconciledBy(String reconciledBy) { this.reconciledBy = reconciledBy; }

    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}

