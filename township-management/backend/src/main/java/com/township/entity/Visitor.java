package com.township.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "visitors",
    indexes = {
        @Index(name = "idx_visitors_flat", columnList = "flat_id"),
        @Index(name = "idx_visitors_resident", columnList = "resident_id"),
        @Index(name = "idx_visitors_pass_code", columnList = "pass_code", unique = true),
        @Index(name = "idx_visitors_pass_token", columnList = "pass_token", unique = true),
        @Index(name = "idx_visitors_status", columnList = "status"),
        @Index(name = "idx_visitors_phone", columnList = "visitor_phone"),
        @Index(name = "idx_visitors_validity", columnList = "valid_from, valid_until")
    }
)
public class Visitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pass_code", nullable = false, unique = true, length = 30)
    private String passCode;

    @Column(name = "pass_token", nullable = false, unique = true, length = 100)
    private String passToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flat_id", nullable = false)
    private Flat flat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id")
    private User resident;

    @Column(name = "visitor_name", nullable = false, length = 100)
    private String visitorName;

    @Column(name = "visitor_phone", nullable = false, length = 25)
    private String visitorPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "visitor_type", nullable = false, length = 30)
    private VisitorType visitorType = VisitorType.GUEST;

    @Column(length = 255)
    private String purpose;

    @Column(name = "vehicle_number", length = 50)
    private String vehicleNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VisitorStatus status = VisitorStatus.PRE_APPROVED;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    @Column(name = "is_one_time", nullable = false)
    private boolean isOneTime = true;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_guard_id")
    private User entryGuard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exit_guard_id")
    private User exitGuard;

    @Column(name = "entry_gate", length = 50)
    private String entryGate = "Gate 1 (Main)";

    @Column(name = "exit_gate", length = 50)
    private String exitGate;

    @Column(name = "photo_storage_path")
    private String photoStoragePath;

    @Column(name = "photo_original_filename")
    private String photoOriginalFilename;

    @Column(name = "photo_size_bytes")
    private Long photoSizeBytes;

    @Column(name = "photo_captured_at")
    private LocalDateTime photoCapturedAt;

    @Column(name = "photo_deleted_at")
    private LocalDateTime photoDeletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "offline_sync_id", unique = true, length = 100)
    private String offlineSyncId;

    @Column(name = "is_offline_sync", nullable = false)
    private boolean isOfflineSync = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Visitor() {}

    public Visitor(
            String passCode,
            String passToken,
            Flat flat,
            User resident,
            String visitorName,
            String visitorPhone,
            VisitorType visitorType,
            String purpose,
            String vehicleNumber,
            VisitorStatus status,
            LocalDateTime validFrom,
            LocalDateTime validUntil,
            boolean isOneTime) {
        this.passCode = passCode;
        this.passToken = passToken;
        this.flat = flat;
        this.resident = resident;
        this.visitorName = visitorName;
        this.visitorPhone = visitorPhone;
        this.visitorType = visitorType;
        this.purpose = purpose;
        this.vehicleNumber = vehicleNumber;
        this.status = status;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.isOneTime = isOneTime;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onPreUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPassCode() { return passCode; }
    public void setPassCode(String passCode) { this.passCode = passCode; }

    public String getPassToken() { return passToken; }
    public void setPassToken(String passToken) { this.passToken = passToken; }

    public Flat getFlat() { return flat; }
    public void setFlat(Flat flat) { this.flat = flat; }

    public User getResident() { return resident; }
    public void setResident(User resident) { this.resident = resident; }

    public String getVisitorName() { return visitorName; }
    public void setVisitorName(String visitorName) { this.visitorName = visitorName; }

    public String getVisitorPhone() { return visitorPhone; }
    public void setVisitorPhone(String visitorPhone) { this.visitorPhone = visitorPhone; }

    public VisitorType getVisitorType() { return visitorType; }
    public void setVisitorType(VisitorType visitorType) { this.visitorType = visitorType; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public VisitorStatus getStatus() { return status; }
    public void setStatus(VisitorStatus status) { this.status = status; }

    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }

    public LocalDateTime getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDateTime validUntil) { this.validUntil = validUntil; }

    public boolean isOneTime() { return isOneTime; }
    public void setOneTime(boolean oneTime) { isOneTime = oneTime; }

    public LocalDateTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalDateTime checkInTime) { this.checkInTime = checkInTime; }

    public LocalDateTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalDateTime checkOutTime) { this.checkOutTime = checkOutTime; }

    public User getEntryGuard() { return entryGuard; }
    public void setEntryGuard(User entryGuard) { this.entryGuard = entryGuard; }

    public User getExitGuard() { return exitGuard; }
    public void setExitGuard(User exitGuard) { this.exitGuard = exitGuard; }

    public String getEntryGate() { return entryGate; }
    public void setEntryGate(String entryGate) { this.entryGate = entryGate; }

    public String getExitGate() { return exitGate; }
    public void setExitGate(String exitGate) { this.exitGate = exitGate; }

    public String getPhotoStoragePath() { return photoStoragePath; }
    public void setPhotoStoragePath(String photoStoragePath) { this.photoStoragePath = photoStoragePath; }

    public String getPhotoOriginalFilename() { return photoOriginalFilename; }
    public void setPhotoOriginalFilename(String photoOriginalFilename) { this.photoOriginalFilename = photoOriginalFilename; }

    public Long getPhotoSizeBytes() { return photoSizeBytes; }
    public void setPhotoSizeBytes(Long photoSizeBytes) { this.photoSizeBytes = photoSizeBytes; }

    public LocalDateTime getPhotoCapturedAt() { return photoCapturedAt; }
    public void setPhotoCapturedAt(LocalDateTime photoCapturedAt) { this.photoCapturedAt = photoCapturedAt; }

    public LocalDateTime getPhotoDeletedAt() { return photoDeletedAt; }
    public void setPhotoDeletedAt(LocalDateTime photoDeletedAt) { this.photoDeletedAt = photoDeletedAt; }

    public User getApprovedBy() { return approvedBy; }
    public void setApprovedBy(User approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getOfflineSyncId() { return offlineSyncId; }
    public void setOfflineSyncId(String offlineSyncId) { this.offlineSyncId = offlineSyncId; }

    public boolean isOfflineSync() { return isOfflineSync; }
    public void setOfflineSync(boolean offlineSync) { isOfflineSync = offlineSync; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

