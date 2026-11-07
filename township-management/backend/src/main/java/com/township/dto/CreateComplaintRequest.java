package com.township.dto;

import com.township.entity.ComplaintCategory;
import com.township.entity.ComplaintPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateComplaintRequest(
    @NotNull(message = "Apartment Flat ID is required")
    Long flatId,

    @NotBlank(message = "Complaint title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    String title,

    @NotBlank(message = "Detailed description is required")
    String description,

    ComplaintCategory category,

    ComplaintPriority priority
) {}

