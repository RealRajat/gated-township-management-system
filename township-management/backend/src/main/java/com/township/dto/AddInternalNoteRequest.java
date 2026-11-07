package com.township.dto;

import jakarta.validation.constraints.NotBlank;

public record AddInternalNoteRequest(
    @NotBlank(message = "Internal note text is required")
    String internalNote
) {}

