package com.township.dto;

public record ComplaintOverviewKpiDto(
    long totalComplaints,
    long openComplaints,
    long criticalEscalations,
    long overdueComplaints,
    long resolvedToday,
    double averageRating
) {}

