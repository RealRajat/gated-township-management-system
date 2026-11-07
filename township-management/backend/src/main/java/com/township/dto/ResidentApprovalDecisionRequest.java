package com.township.dto;

import jakarta.validation.constraints.NotNull;

public record ResidentApprovalDecisionRequest(
    @NotNull(message = "Approval decision is required")
    Boolean approved,

    String rejectionReason
) {}

