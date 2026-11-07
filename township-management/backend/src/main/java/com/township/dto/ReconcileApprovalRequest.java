package com.township.dto;

import jakarta.validation.constraints.NotBlank;

public record ReconcileApprovalRequest(
    Long bankTransactionId,
    Long transactionId,
    Long targetBillId,
    Long targetFlatId,
    @NotBlank(message = "Action is required (APPROVE or REJECT)")
    String action,
    String notes
) {
    public Long resolvedTransactionId() {
        return bankTransactionId != null ? bankTransactionId : transactionId;
    }
}
