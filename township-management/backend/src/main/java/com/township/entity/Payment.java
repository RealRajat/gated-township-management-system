package com.township.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    private MaintenanceBill bill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flat_id", nullable = false)
    private Flat flat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate = LocalDateTime.now();

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod;

    @Column(name = "reference_number", nullable = false, unique = true, length = 100)
    private String referenceNumber;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "COMPLETED";

    @Column(name = "notes")
    private String notes;

    @Column(name = "is_test_mode", nullable = false)
    private boolean isTestMode = true;

    @Column(name = "receipt_number", length = 100)
    private String receiptNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Payment() {}

    public Payment(
            MaintenanceBill bill,
            Flat flat,
            User user,
            BigDecimal amount,
            LocalDateTime paymentDate,
            String paymentMethod,
            String referenceNumber,
            String status,
            String notes) {
        this.bill = bill;
        this.flat = flat;
        this.user = user;
        this.amount = amount;
        this.paymentDate = paymentDate != null ? paymentDate : LocalDateTime.now();
        this.paymentMethod = paymentMethod;
        this.referenceNumber = referenceNumber;
        this.status = status != null ? status : "COMPLETED";
        this.notes = notes;
        this.isTestMode = true;
        this.createdAt = LocalDateTime.now();
    }

    public Payment(
            MaintenanceBill bill,
            Flat flat,
            User user,
            BigDecimal amount,
            LocalDateTime paymentDate,
            String paymentMethod,
            String referenceNumber,
            String status,
            String notes,
            boolean isTestMode,
            String receiptNumber) {
        this.bill = bill;
        this.flat = flat;
        this.user = user;
        this.amount = amount;
        this.paymentDate = paymentDate != null ? paymentDate : LocalDateTime.now();
        this.paymentMethod = paymentMethod;
        this.referenceNumber = referenceNumber;
        this.status = status != null ? status : "COMPLETED";
        this.notes = notes;
        this.isTestMode = isTestMode;
        this.receiptNumber = receiptNumber;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MaintenanceBill getBill() { return bill; }
    public void setBill(MaintenanceBill bill) { this.bill = bill; }

    public Flat getFlat() { return flat; }
    public void setFlat(Flat flat) { this.flat = flat; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isTestMode() { return isTestMode; }
    public void setTestMode(boolean testMode) { isTestMode = testMode; }

    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
