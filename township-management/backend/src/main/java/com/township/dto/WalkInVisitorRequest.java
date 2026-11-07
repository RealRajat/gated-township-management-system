package com.township.dto;

import com.township.entity.VisitorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WalkInVisitorRequest(
    @NotNull(message = "Flat ID is required")
    Long flatId,

    @NotBlank(message = "Visitor name is required")
    String visitorName,

    @NotBlank(message = "Visitor mobile phone is required")
    String visitorPhone,

    @NotNull(message = "Visitor type is required")
    VisitorType visitorType,

    String purpose,
    String vehicleNumber,
    String gate
) {}

