package com.township.dto;

import com.township.entity.AmenityCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

public record CreateAmenityRequest(
    @NotBlank(message = "Amenity name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    String name,

    @NotBlank(message = "Amenity code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    String code,

    @NotNull(message = "Amenity category is required")
    AmenityCategory category,

    String description,

    String location,

    @NotNull(message = "Open time is required")
    LocalTime openTime,

    @NotNull(message = "Close time is required")
    LocalTime closeTime,

    @NotNull(message = "Slot duration in minutes is required")
    @Positive(message = "Slot duration must be positive")
    Integer slotDurationMinutes,

    @NotNull(message = "Slot capacity is required")
    @Positive(message = "Slot capacity must be positive")
    Integer slotCapacity,

    @NotNull(message = "Max active bookings per flat is required")
    @Positive(message = "Max active bookings must be positive")
    Integer maxActiveBookingsPerFlat,

    @NotNull(message = "Cancellation deadline in hours is required")
    Integer cancelDeadlineHours,

    String rulesAndGuidelines
) {}

