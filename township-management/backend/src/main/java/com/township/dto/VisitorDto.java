package com.township.dto;

import com.township.entity.VisitorStatus;
import com.township.entity.VisitorType;
import java.time.LocalDateTime;
import java.util.List;

public record VisitorDto(
    Long id,
    String passCode,
    String passToken,
    Long flatId,
    String towerId,
    String towerName,
    String flatNumber,
    Long residentId,
    String residentName,
    String residentPhone,
    String visitorName,
    String visitorPhone,
    VisitorType visitorType,
    String purpose,
    String vehicleNumber,
    VisitorStatus status,
    LocalDateTime validFrom,
    LocalDateTime validUntil,
    boolean isOneTime,
    LocalDateTime checkInTime,
    LocalDateTime checkOutTime,
    String entryGate,
    String exitGate,
    String entryGuardName,
    String exitGuardName,
    boolean hasPhoto,
    String photoUrl,
    LocalDateTime photoCapturedAt,
    String approvedByName,
    LocalDateTime approvedAt,
    String rejectionReason,
    boolean isOfflineSync,
    LocalDateTime createdAt,
    List<VisitorActivityDto> activities
) {}

