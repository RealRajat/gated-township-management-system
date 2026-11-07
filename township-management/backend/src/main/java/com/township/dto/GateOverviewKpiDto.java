package com.township.dto;

public record GateOverviewKpiDto(
    long currentlyInside,
    long todayTotalCheckIns,
    long pendingApprovals,
    long preApprovedUpcoming
) {}

