package com.township.dto;

import java.util.Map;

public record ComplaintAnalyticsDto(
        long totalComplaints,
        long openCount,
        long assignedCount,
        long inProgressCount,
        long onHoldCount,
        long resolvedCount,
        long closedCount,
        double avgFirstResponseHours,
        double avgResolutionHours,
        long slaBreachCount,
        long criticalEscalationsCount,
        Map<String, Long> categoryBreakdown,
        Map<String, Long> priorityBreakdown
) {}

