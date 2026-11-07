package com.township.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "maintenance_bills",
    uniqueConstraints = {
        @UniqueConstraint(name = "unique_flat_billing_month", columnNames = {"flat_id", "billing_month"})
    }
)
public class MaintenanceBill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flat_id", nullable = false)
    private Flat flat;

    @Column(name = "billing_month", nullable = false, length = 7)
    private String billingMonth; // Format: YYYY-MM

    @Column(name = "flat_area_sq_ft", nullable = false, precision = 10, scale = 2)
    private BigDecimal flatAreaSqFt;

    @Column(name = "rate_per_sq_ft", nullable = false, precision = 10, scale = 2)
    private BigDecimal ratePerSqFt;

    @Column(name = "base_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseAmount;

    @Column(name = "late_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal lateFee = BigDecimal.ZERO;

    @Column(name = "total_due", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalDue;

    @Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "outstanding_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal outstandingBalance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BillStatus status = BillStatus.UNPAID;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public MaintenanceBill() {}

    public MaintenanceBill(
            Flat flat,
            String billingMonth,
            BigDecimal flatAreaSqFt,
            BigDecimal ratePerSqFt,
            BigDecimal baseAmount,
            BigDecimal lateFee,
            BigDecimal totalDue,
            BigDecimal paidAmount,
            BigDecimal outstandingBalance,
            BillStatus status,
            LocalDate issueDate,
            LocalDate dueDate,
            String notes) {
        this.flat = flat;
        this.billingMonth = billingMonth;
        this.flatAreaSqFt = flatAreaSqFt;
        this.ratePerSqFt = ratePerSqFt;
        this.baseAmount = baseAmount;
        this.lateFee = lateFee != null ? lateFee : BigDecimal.ZERO;
        this.totalDue = totalDue;
        this.paidAmount = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        this.outstandingBalance = outstandingBalance;
        this.status = status;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.notes = notes;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Flat getFlat() { return flat; }
    public void setFlat(Flat flat) { this.flat = flat; }

    public String getBillingMonth() { return billingMonth; }
    public void setBillingMonth(String billingMonth) { this.billingMonth = billingMonth; }

    public BigDecimal getFlatAreaSqFt() { return flatAreaSqFt; }
    public void setFlatAreaSqFt(BigDecimal flatAreaSqFt) { this.flatAreaSqFt = flatAreaSqFt; }

    public BigDecimal getRatePerSqFt() { return ratePerSqFt; }
    public void setRatePerSqFt(BigDecimal ratePerSqFt) { this.ratePerSqFt = ratePerSqFt; }

    public BigDecimal getBaseAmount() { return baseAmount; }
    public void setBaseAmount(BigDecimal baseAmount) { this.baseAmount = baseAmount; }

    public BigDecimal getLateFee() { return lateFee; }
    public void setLateFee(BigDecimal lateFee) { this.lateFee = lateFee; }

    public BigDecimal getTotalDue() { return totalDue; }
    public void setTotalDue(BigDecimal totalDue) { this.totalDue = totalDue; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public BigDecimal getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(BigDecimal outstandingBalance) { this.outstandingBalance = outstandingBalance; }

    public BillStatus getStatus() { return status; }
    public void setStatus(BillStatus status) { this.status = status; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

