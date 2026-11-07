package com.township.dto;

import jakarta.validation.constraints.NotNull;

public record ToggleAmenityStatusRequest(
    @NotNull(message = "Active status is required")
    Boolean isActive,

    String maintenanceReason
) {}

