package com.township.dto;

public record FlatSummaryDto(
    Long flatId,
    String towerId,
    String towerName,
    String flatNumber,
    int floorNumber,
    double areaSqFt,
    String occupancyStatus,
    String memberType
) {}
