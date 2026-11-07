package com.township.dto;

import com.township.entity.NoticeCategory;
import com.township.entity.NoticePriority;
import com.township.entity.NoticeStatus;
import java.time.LocalDateTime;

public record NoticeDto(
    Long id,
    String title,
    String description,
    NoticeCategory category,
    NoticePriority priority,
    NoticeStatus status,
    String targetTowerId,
    String targetTowerName,
    boolean isTownshipWide,
    boolean isPinned,
    LocalDateTime publishDate,
    LocalDateTime expiryDate,
    String authorName,
    boolean isRead,
    LocalDateTime readAt,
    long readCount,
    boolean isExpired,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

