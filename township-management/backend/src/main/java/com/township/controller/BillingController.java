package com.township.controller;

import com.township.dto.*;
import com.township.entity.BillStatus;
import com.township.service.BillingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    /**
     * Resident's own maintenance bills & summary
     */
    @GetMapping("/my-bills")
    public ResponseEntity<ResidentBillingSummaryDto> getMyBills(Authentication authentication) {
        ResidentBillingSummaryDto summary = billingService.getResidentBillingSummary(authentication.getName());
        return ResponseEntity.ok(summary);
    }

    /**
     * Individual bill details (Access controlled: Resident must be linked to the flat, or HELP_DESK)
     */
    @GetMapping("/bills/{billId}")
    public ResponseEntity<MaintenanceBillDto> getBillById(
            @PathVariable Long billId,
            Authentication authentication) {
        MaintenanceBillDto bill = billingService.getBillById(billId, authentication.getName());
        return ResponseEntity.ok(bill);
    }

    /**
     * Flat ledger history (Access controlled: Resident must be linked to the flat, or HELP_DESK)
     */
    @GetMapping("/ledger/{flatId}")
    public ResponseEntity<List<FlatLedgerEntryDto>> getFlatLedger(
            @PathVariable Long flatId,
            Authentication authentication) {
        List<FlatLedgerEntryDto> ledger = billingService.getFlatLedger(flatId, authentication.getName());
        return ResponseEntity.ok(ledger);
    }

    /**
     * Help Desk: Township-wide billing overview metrics
     */
    @GetMapping("/admin/overview")
    @PreAuthorize("hasAuthority('ROLE_HELP_DESK')")
    public ResponseEntity<BillingOverviewDto> getBillingOverview() {
        BillingOverviewDto overview = billingService.getBillingOverview();
        return ResponseEntity.ok(overview);
    }

    /**
     * Help Desk: Filterable bills table across all 28 towers
     */
    @GetMapping("/admin/bills")
    @PreAuthorize("hasAuthority('ROLE_HELP_DESK')")
    public ResponseEntity<List<MaintenanceBillDto>> getAllBills(
            @RequestParam(required = false) String billingMonth,
            @RequestParam(required = false) BillStatus status,
            @RequestParam(required = false) String towerId) {
        List<MaintenanceBillDto> bills = billingService.getAllBills(billingMonth, status, towerId);
        return ResponseEntity.ok(bills);
    }

    /**
     * Help Desk: Generate monthly bills idempotently across all flats
     */
    @PostMapping("/admin/generate-monthly")
    @PreAuthorize("hasAuthority('ROLE_HELP_DESK')")
    public ResponseEntity<GenerateBillsResponse> generateMonthlyBills(
            @Valid @RequestBody GenerateBillsRequest request) {
        GenerateBillsResponse response = billingService.generateMonthlyBills(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Help Desk: Get active billing configuration
     */
    @GetMapping("/admin/config")
    @PreAuthorize("hasAuthority('ROLE_HELP_DESK')")
    public ResponseEntity<BillingConfigDto> getBillingConfig() {
        return ResponseEntity.ok(billingService.getBillingConfig());
    }

    /**
     * Help Desk: Update billing configuration
     */
    @PutMapping("/admin/config")
    @PreAuthorize("hasAuthority('ROLE_HELP_DESK')")
    public ResponseEntity<BillingConfigDto> updateBillingConfig(@Valid @RequestBody BillingConfigDto dto) {
        return ResponseEntity.ok(billingService.updateBillingConfig(dto));
    }
}

