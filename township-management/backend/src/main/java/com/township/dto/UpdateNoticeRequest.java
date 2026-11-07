package com.township.dto;

import com.township.entity.NoticeCategory;
import com.township.entity.NoticePriority;
import com.township.entity.NoticeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record UpdateNoticeRequest(
    @NotBlank(message = "Notice title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    String title,

    @NotBlank(message = "Notice description is required")
    String description,

    @NotNull(message = "Notice category is required")
    NoticeCategory category,

    @NotNull(message = "Notice priority is required")
    NoticePriority priority,

    @NotNull(message = "Notice status is required")
    NoticeStatus status,

    String targetTowerId, // null or empty string = Township-Wide

    Boolean isPinned,

    LocalDateTime publishDate,

    LocalDateTime expiryDate
) {}

