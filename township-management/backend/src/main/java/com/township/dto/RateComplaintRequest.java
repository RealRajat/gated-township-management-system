package com.township.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record RateComplaintRequest(
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    int rating,

    String feedback
) {}

