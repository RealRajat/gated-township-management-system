package com.township.dto;

import com.township.entity.VisitorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record PreApproveVisitorRequest(
    @NotNull(message = "Flat ID is required")
    Long flatId,

    @NotBlank(message = "Visitor name is required")
    String visitorName,

    @NotBlank(message = "Visitor mobile phone is required")
    String visitorPhone,

    @NotNull(message = "Visitor type is required")
    VisitorType visitorType,

    @NotNull(message = "Valid from timestamp is required")
    LocalDateTime validFrom,

    @NotNull(message = "Valid until timestamp is required")
    LocalDateTime validUntil,

    String purpose,
    String vehicleNumber,
    Boolean isOneTime
) {}

