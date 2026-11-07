package com.township.dto;

import com.township.entity.VisitorStatus;
import com.township.entity.VisitorType;
import java.time.LocalDateTime;

public record GateActivePassDto(
    String passCode,
    String passToken,
    String visitorName,
    String visitorPhone,
    VisitorType visitorType,
    String towerName,
    String flatNumber,
    LocalDateTime validFrom,
    LocalDateTime validUntil,
    boolean isOneTime,
    VisitorStatus status
) {}

