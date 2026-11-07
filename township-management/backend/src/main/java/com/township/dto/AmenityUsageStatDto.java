package com.township.dto;

public record AmenityUsageStatDto(
        Long amenityId,
        String amenityName,
        String category,
        long bookingCount,
        long totalAttendees
) {}

