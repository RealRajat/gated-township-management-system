package com.township.dto;

public record NoticeOverviewKpiDto(
    long totalNotices,
    long activePublished,
    long emergencyNotices,
    long pinnedNotices,
    long unreadCount
) {}

