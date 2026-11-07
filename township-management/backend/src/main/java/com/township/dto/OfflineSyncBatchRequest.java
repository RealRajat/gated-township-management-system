package com.township.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record OfflineSyncBatchRequest(
    @NotEmpty(message = "Events list cannot be empty")
    List<@Valid OfflineSyncItemDto> events
) {}

