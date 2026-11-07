package com.township.dto;

import com.township.entity.ComplaintStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateComplaintStatusRequest(
    @NotNull(message = "New complaint status is required")
    ComplaintStatus newStatus,

    String resolutionNotes,

    String internalNotes
) {}

