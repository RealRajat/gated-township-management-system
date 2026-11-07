package com.township.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record OfflineSyncItemDto(
    @NotBlank(message = "Offline sync idempotency ID is required")
    String offlineSyncId,

    @NotBlank(message = "Action type is required (CHECK_IN or CHECK_OUT)")
    String action,

    @NotBlank(message = "Pass code or token is required")
    String passCodeOrToken,

    @NotNull(message = "Offline timestamp is required")
    LocalDateTime timestamp,

    String gate,
    String vehicleNumber
) {}

