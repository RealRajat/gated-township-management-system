package com.township.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TestPaymentRequest(
    @NotNull(message = "Bill ID is required")
    Long billId,

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "1.00", message = "Payment amount must be at least ₹1.00")
    BigDecimal amount,

    @NotBlank(message = "Payment method is required")
    String paymentMethod, // UPI, NET_BANKING, DEBIT_CARD, CREDIT_CARD

    String idempotencyKey,

    Boolean simulateFailure,

    String failureReason
) {}

