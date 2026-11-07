package com.township.dto;

public record TowerDto(
    String id,
    String name,
    int floors,
    int totalUnits,
    String type
) {}
