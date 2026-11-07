package com.township.dto;

import java.time.LocalTime;

public record SlotAvailabilityDto(
    LocalTime startTime,
    LocalTime endTime,
    String slotLabel,
    int totalCapacity,
    int bookedCount,
    int availableSpots,
    boolean isPast,
    boolean isAvailable,
    boolean isBookedByMyFlat
) {}

