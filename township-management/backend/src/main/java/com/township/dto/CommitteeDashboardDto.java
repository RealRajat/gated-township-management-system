package com.township.dto;

import java.util.List;

public record CommitteeDashboardDto(
        CommitteeFinancialKpiDto financial,
        List<CollectionTrendDto> collectionTrends,
        List<DuesAgeingBucketDto> duesAgeing,
        List<TowerDuesSummaryDto> towerDues,
        ComplaintAnalyticsDto complaints,
        OperationalAnalyticsDto operational
) {}

