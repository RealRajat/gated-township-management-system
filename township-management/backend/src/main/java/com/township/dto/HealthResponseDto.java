package com.township.dto;

import java.time.Instant;
import java.util.Map;

public record HealthResponseDto(
    String status,
    String service,
    String version,
    String environment,
    Instant timestamp,
    Map<String, Object> database,
    Map<String, Object> system
) {}

