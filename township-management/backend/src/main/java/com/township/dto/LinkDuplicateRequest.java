package com.township.dto;

import jakarta.validation.constraints.NotNull;

public record LinkDuplicateRequest(
    @NotNull(message = "Master complaint ID is required")
    Long masterComplaintId,

    String notes
) {}

