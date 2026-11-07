package com.township.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "billing_configurations")
public class BillingConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "base_rate_per_sq_ft", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseRatePerSqFt = new BigDecimal("6.50");

    @Column(name = "grace_period_days", nullable = false)
    private Integer gracePeriodDays = 15;

    @Column(name = "late_fee_type", nullable = false, length = 20)
    private String lateFeeType = "PERCENTAGE";

    @Column(name = "late_fee_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal lateFeePercentage = new BigDecimal("10.00");

    @Column(name = "late_fee_fixed_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal lateFeeFixedAmount = BigDecimal.ZERO;

    @Column(name = "sinking_fund_per_flat", nullable = false, precision = 10, scale = 2)
    private BigDecimal sinkingFundPerFlat = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public BillingConfiguration() {}

    public BillingConfiguration(
            BigDecimal baseRatePerSqFt,
            Integer gracePeriodDays,
            String lateFeeType,
            BigDecimal lateFeePercentage,
            BigDecimal lateFeeFixedAmount,
            BigDecimal sinkingFundPerFlat,
            boolean active) {
        this.baseRatePerSqFt = baseRatePerSqFt != null ? baseRatePerSqFt : new BigDecimal("6.50");
        this.gracePeriodDays = gracePeriodDays != null ? gracePeriodDays : 15;
        this.lateFeeType = lateFeeType != null ? lateFeeType : "PERCENTAGE";
        this.lateFeePercentage = lateFeePercentage != null ? lateFeePercentage : new BigDecimal("10.00");
        this.lateFeeFixedAmount = lateFeeFixedAmount != null ? lateFeeFixedAmount : BigDecimal.ZERO;
        this.sinkingFundPerFlat = sinkingFundPerFlat != null ? sinkingFundPerFlat : BigDecimal.ZERO;
        this.active = active;
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BigDecimal getBaseRatePerSqFt() { return baseRatePerSqFt; }
    public void setBaseRatePerSqFt(BigDecimal baseRatePerSqFt) { this.baseRatePerSqFt = baseRatePerSqFt; }

    public Integer getGracePeriodDays() { return gracePeriodDays; }
    public void setGracePeriodDays(Integer gracePeriodDays) { this.gracePeriodDays = gracePeriodDays; }

    public String getLateFeeType() { return lateFeeType; }
    public void setLateFeeType(String lateFeeType) { this.lateFeeType = lateFeeType; }

    public BigDecimal getLateFeePercentage() { return lateFeePercentage; }
    public void setLateFeePercentage(BigDecimal lateFeePercentage) { this.lateFeePercentage = lateFeePercentage; }

    public BigDecimal getLateFeeFixedAmount() { return lateFeeFixedAmount; }
    public void setLateFeeFixedAmount(BigDecimal lateFeeFixedAmount) { this.lateFeeFixedAmount = lateFeeFixedAmount; }

    public BigDecimal getSinkingFundPerFlat() { return sinkingFundPerFlat; }
    public void setSinkingFundPerFlat(BigDecimal sinkingFundPerFlat) { this.sinkingFundPerFlat = sinkingFundPerFlat; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

