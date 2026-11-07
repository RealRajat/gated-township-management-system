package com.township.dto;

import java.time.LocalDateTime;

public record VisitorActivityDto(
    Long id,
    String actionType,
    String performedByName,
    String details,
    LocalDateTime timestamp
) {}

