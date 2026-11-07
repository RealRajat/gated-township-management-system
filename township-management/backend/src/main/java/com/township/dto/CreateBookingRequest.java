package com.township.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateBookingRequest(
    @NotNull(message = "Amenity ID is required")
    Long amenityId,

    @NotNull(message = "Apartment Flat ID is required")
    Long flatId,

    @NotNull(message = "Booking date is required")
    LocalDate bookingDate,

    @NotNull(message = "Start time is required")
    LocalTime startTime,

    @NotNull(message = "Attendee count is required")
    @Positive(message = "Attendee count must be at least 1")
    Integer attendeeCount,

    @Size(max = 255, message = "Purpose must not exceed 255 characters")
    String purpose
) {}

