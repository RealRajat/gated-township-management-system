package com.township.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
    name = "amenities",
    indexes = {
        @Index(name = "idx_amenities_code", columnList = "code", unique = true),
        @Index(name = "idx_amenities_category", columnList = "category"),
        @Index(name = "idx_amenities_is_active", columnList = "is_active")
    }
)
public class Amenity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AmenityCategory category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String location;

    @Column(name = "open_time", nullable = false)
    private LocalTime openTime = LocalTime.of(6, 0);

    @Column(name = "close_time", nullable = false)
    private LocalTime closeTime = LocalTime.of(22, 0);

    @Column(name = "slot_duration_minutes", nullable = false)
    private Integer slotDurationMinutes = 60;

    @Column(name = "slot_capacity", nullable = false)
    private Integer slotCapacity = 10;

    @Column(name = "max_active_bookings_per_flat", nullable = false)
    private Integer maxActiveBookingsPerFlat = 2;

    @Column(name = "cancel_deadline_hours", nullable = false)
    private Integer cancelDeadlineHours = 2;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "maintenance_reason", length = 255)
    private String maintenanceReason;

    @Column(name = "rules_and_guidelines", columnDefinition = "TEXT")
    private String rulesAndGuidelines;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Amenity() {
    }

    public Amenity(String name, String code, AmenityCategory category, String description, String location,
                   LocalTime openTime, LocalTime closeTime, Integer slotDurationMinutes, Integer slotCapacity,
                   Integer maxActiveBookingsPerFlat, Integer cancelDeadlineHours, String rulesAndGuidelines) {
        this.name = name;
        this.code = code;
        this.category = category;
        this.description = description;
        this.location = location;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.slotDurationMinutes = slotDurationMinutes;
        this.slotCapacity = slotCapacity;
        this.maxActiveBookingsPerFlat = maxActiveBookingsPerFlat;
        this.cancelDeadlineHours = cancelDeadlineHours;
        this.rulesAndGuidelines = rulesAndGuidelines;
        this.isActive = true;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public AmenityCategory getCategory() {
        return category;
    }

    public void setCategory(AmenityCategory category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalTime getOpenTime() {
        return openTime;
    }

    public void setOpenTime(LocalTime openTime) {
        this.openTime = openTime;
    }

    public LocalTime getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(LocalTime closeTime) {
        this.closeTime = closeTime;
    }

    public Integer getSlotDurationMinutes() {
        return slotDurationMinutes;
    }

    public void setSlotDurationMinutes(Integer slotDurationMinutes) {
        this.slotDurationMinutes = slotDurationMinutes;
    }

    public Integer getSlotCapacity() {
        return slotCapacity;
    }

    public void setSlotCapacity(Integer slotCapacity) {
        this.slotCapacity = slotCapacity;
    }

    public Integer getMaxActiveBookingsPerFlat() {
        return maxActiveBookingsPerFlat;
    }

    public void setMaxActiveBookingsPerFlat(Integer maxActiveBookingsPerFlat) {
        this.maxActiveBookingsPerFlat = maxActiveBookingsPerFlat;
    }

    public Integer getCancelDeadlineHours() {
        return cancelDeadlineHours;
    }

    public void setCancelDeadlineHours(Integer cancelDeadlineHours) {
        this.cancelDeadlineHours = cancelDeadlineHours;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public String getMaintenanceReason() {
        return maintenanceReason;
    }

    public void setMaintenanceReason(String maintenanceReason) {
        this.maintenanceReason = maintenanceReason;
    }

    public String getRulesAndGuidelines() {
        return rulesAndGuidelines;
    }

    public void setRulesAndGuidelines(String rulesAndGuidelines) {
        this.rulesAndGuidelines = rulesAndGuidelines;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

