package com.township.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignComplaintRequest(
    @NotBlank(message = "Assigned team or contractor is required")
    String assignedTeam,

    Long assignedStaffId,

    String notes
) {}

