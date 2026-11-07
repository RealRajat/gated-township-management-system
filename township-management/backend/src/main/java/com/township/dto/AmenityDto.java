package com.township.dto;

import com.township.entity.AmenityCategory;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record AmenityDto(
    Long id,
    String name,
    String code,
    AmenityCategory category,
    String description,
    String location,
    LocalTime openTime,
    LocalTime closeTime,
    Integer slotDurationMinutes,
    Integer slotCapacity,
    Integer maxActiveBookingsPerFlat,
    Integer cancelDeadlineHours,
    Boolean isActive,
    String maintenanceReason,
    String rulesAndGuidelines,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

