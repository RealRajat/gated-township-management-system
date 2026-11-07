package com.township.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GenerateBillsRequest(
    @NotBlank(message = "Billing month is required")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "Billing month must be in YYYY-MM format (e.g. 2026-10)")
    String billingMonth,

    Integer dueDays,

    String notes
) {}

