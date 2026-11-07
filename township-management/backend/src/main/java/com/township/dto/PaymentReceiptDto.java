package com.township.dto;

import com.township.entity.BillStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentReceiptDto(
    Long paymentId,
    String receiptNumber,
    String transactionReference,
    String referenceNumber,
    Long billId,
    String billingMonth,
    Long flatId,
    String towerId,
    String towerName,
    String flatNumber,
    String residentName,
    String residentEmail,
    BigDecimal amountPaid,
    LocalDateTime paymentDate,
    String paymentMethod,
    BigDecimal previousOutstanding,
    BigDecimal remainingBalance,
    BillStatus billStatus,
    boolean isTestMode,
    String watermark,
    String testModeNotice
) {}
