package com.township.dto;

public record AmenityOverviewKpiDto(
    long totalAmenities,
    long activeAmenities,
    long bookingsToday,
    long upcomingBookingsWeek,
    long cancelledBookingsMonth
) {}

