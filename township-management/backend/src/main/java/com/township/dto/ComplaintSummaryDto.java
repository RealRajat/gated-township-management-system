package com.township.dto;

import com.township.entity.ComplaintCategory;
import com.township.entity.ComplaintPriority;
import com.township.entity.ComplaintStatus;
import java.time.LocalDateTime;

public record ComplaintSummaryDto(
    Long id,
    String complaintNumber,
    Long flatId,
    String towerId,
    String towerName,
    String flatNumber,
    String residentName,
    String residentPhone,
    String title,
    ComplaintCategory category,
    ComplaintPriority priority,
    ComplaintStatus status,
    boolean isCriticalSafetyEscalation,
    String safetyTriggerKeyword,
    String assignedTeam,
    String assignedStaffName,
    boolean hasPhoto,
    LocalDateTime createdAt,
    LocalDateTime responseDeadline,
    LocalDateTime resolutionDeadline,
    boolean isOverdue,
    Integer residentRating,
    Long parentDuplicateOfId,
    String parentDuplicateComplaintNumber
) {}

