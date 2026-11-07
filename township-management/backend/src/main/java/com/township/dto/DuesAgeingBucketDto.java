package com.township.dto;

import java.math.BigDecimal;

public record DuesAgeingBucketDto(
        String bucket,
        long billCount,
        BigDecimal totalOutstanding,
        double percentage
) {}

