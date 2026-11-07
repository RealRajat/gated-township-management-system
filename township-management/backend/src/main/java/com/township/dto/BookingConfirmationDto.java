package com.township.dto;

import com.township.entity.AmenityCategory;
import com.township.entity.BookingStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record BookingConfirmationDto(
    Long id,
    String bookingReference,
    Long amenityId,
    String amenityName,
    String amenityCode,
    AmenityCategory amenityCategory,
    String amenityLocation,
    Long flatId,
    String towerId,
    String towerName,
    String flatNumber,
    Long residentId,
    String residentName,
    String residentPhone,
    LocalDate bookingDate,
    LocalTime startTime,
    LocalTime endTime,
    String slotLabel,
    int attendeeCount,
    String purpose,
    BookingStatus status,
    String cancellationReason,
    LocalDateTime cancelledAt,
    String cancelledByName,
    LocalDateTime cancellationDeadline,
    boolean canCancel,
    String rulesAndGuidelines,
    LocalDateTime createdAt
) {}

