package com.township.dto;

import com.township.entity.ComplaintStatus;
import java.time.LocalDateTime;

public record ComplaintActivityDto(
    Long id,
    String actorName,
    String actorRole,
    ComplaintStatus oldStatus,
    ComplaintStatus newStatus,
    String activityType,
    String publicNote,
    String internalNote,
    boolean isInternalOnly,
    LocalDateTime timestamp
) {}

