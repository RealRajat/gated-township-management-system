package com.township.dto;

import com.township.entity.ComplaintCategory;
import com.township.entity.ComplaintPriority;

public record ComplaintClassificationDto(
    ComplaintCategory suggestedCategory,
    ComplaintPriority suggestedPriority,
    int confidenceScore,
    boolean isSafetyEscalation,
    String safetyTriggerKeyword,
    String reason,
    String provider
) {}

