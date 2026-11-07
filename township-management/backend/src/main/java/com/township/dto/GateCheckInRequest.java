package com.township.dto;

import jakarta.validation.constraints.NotBlank;

public record GateCheckInRequest(
    @NotBlank(message = "Pass code or secure QR token is required")
    String passCodeOrToken,

    String gate,
    String vehicleNumber
) {}

