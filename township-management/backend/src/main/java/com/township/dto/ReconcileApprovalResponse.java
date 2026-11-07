package com.township.dto;

import com.township.entity.BillStatus;
import java.math.BigDecimal;

public record ReconcileApprovalResponse(
    Long bankTransactionId,
    String status,
    BigDecimal reconciledAmount,
    Long billId,
    String flatNumber,
    BillStatus newBillStatus,
    String message
) {}

