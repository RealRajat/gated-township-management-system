package com.township.dto;

import java.util.List;

public record OfflineSyncBatchResponse(
    int totalReceived,
    int successfullySynced,
    int duplicatesSkipped,
    int failed,
    List<String> messages
) {}

