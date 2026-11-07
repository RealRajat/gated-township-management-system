package com.township.dto;

import jakarta.validation.constraints.Size;

public record CancelBookingRequest(
    @Size(max = 255, message = "Cancellation reason must not exceed 255 characters")
    String reason
) {}

