package com.township.dto;

import java.util.List;

public record OperationalAnalyticsDto(
        long totalVisitors,
        long todayCheckIns,
        long checkedInCurrently,
        long checkedOut,
        long preApprovedCount,
        long walkInCount,
        long totalAmenityBookings,
        long confirmedBookings,
        long cancelledBookings,
        double cancellationRate,
        List<AmenityUsageStatDto> topAmenities,
        long totalNoticesPublished,
        long activeNotices,
        long totalNoticeReads,
        double noticeEngagementRate,
        List<TowerOperationalSummaryDto> towerOperationalComparison
) {}

