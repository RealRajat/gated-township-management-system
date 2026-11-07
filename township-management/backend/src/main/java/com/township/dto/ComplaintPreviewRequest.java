package com.township.dto;

import jakarta.validation.constraints.NotBlank;

public record ComplaintPreviewRequest(
    String title,
    @NotBlank(message = "Description is required for classification")
    String description,
    Long flatId
) {}

