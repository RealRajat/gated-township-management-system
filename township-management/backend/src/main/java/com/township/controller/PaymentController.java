package com.township.controller;

import com.township.dto.PaymentHistoryItemDto;
import com.township.dto.PaymentReceiptDto;
import com.township.dto.TestPaymentRequest;
import com.township.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Process a test mode payment against an outstanding maintenance bill
     */
    @PostMapping("/test-pay")
    public ResponseEntity<PaymentReceiptDto> processTestPayment(
            @Valid @RequestBody TestPaymentRequest request,
            Authentication authentication) {
        PaymentReceiptDto receipt = paymentService.processTestPayment(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(receipt);
    }

    /**
     * Resident payment history
     */
    @GetMapping("/my-history")
    public ResponseEntity<List<PaymentHistoryItemDto>> getMyPaymentHistory(Authentication authentication) {
        List<PaymentHistoryItemDto> history = paymentService.getResidentPaymentHistory(authentication.getName());
        return ResponseEntity.ok(history);
    }

    /**
     * Download or view payment receipt
     */
    @GetMapping("/receipt/{paymentId}")
    public ResponseEntity<PaymentReceiptDto> getReceipt(
            @PathVariable Long paymentId,
            Authentication authentication) {
        PaymentReceiptDto receipt = paymentService.getPaymentReceipt(paymentId, authentication.getName());
        return ResponseEntity.ok(receipt);
    }

    /**
     * Help Desk: View all payments across the township
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ROLE_HELP_DESK')")
    public ResponseEntity<List<PaymentHistoryItemDto>> getAllPayments(
            @RequestParam(required = false) String towerId,
            @RequestParam(required = false) String paymentMethod) {
        List<PaymentHistoryItemDto> payments = paymentService.getAllPayments(towerId, paymentMethod);
        return ResponseEntity.ok(payments);
    }
}

