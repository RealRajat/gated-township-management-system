package com.township.controller;

import com.township.dto.BankBatchSummaryDto;
import com.township.dto.BankTransactionDto;
import com.township.dto.ReconcileApprovalRequest;
import com.township.dto.ReconcileApprovalResponse;
import com.township.entity.BankMatchStatus;
import com.township.service.BankReconciliationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/reconciliation")
public class BankReconciliationController {

    private final BankReconciliationService reconciliationService;

    public BankReconciliationController(BankReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    /**
     * Upload Bank Statement CSV (HELP_DESK only)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_HELP_DESK')")
    public ResponseEntity<BankBatchSummaryDto> uploadBankStatement(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        BankBatchSummaryDto summary = reconciliationService.importBankStatementCsv(file, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(summary);
    }

    /**
     * Get imported bank statement transactions with status filtering (HELP_DESK only)
     */
    @GetMapping("/transactions")
    @PreAuthorize("hasAuthority('ROLE_HELP_DESK')")
    public ResponseEntity<List<BankTransactionDto>> getTransactions(
            @RequestParam(required = false) BankMatchStatus status,
            @RequestParam(required = false) String batchId) {
        List<BankTransactionDto> list = reconciliationService.getBankTransactions(status, batchId);
        return ResponseEntity.ok(list);
    }

    /**
     * Approve or reject a matched/ambiguous bank transaction (HELP_DESK only)
     */
    @PostMapping("/approve")
    @PreAuthorize("hasAuthority('ROLE_HELP_DESK')")
    public ResponseEntity<ReconcileApprovalResponse> approveOrRejectTransaction(
            @Valid @RequestBody ReconcileApprovalRequest request,
            Authentication authentication) {
        ReconcileApprovalResponse response = reconciliationService.reconcileAndApprove(request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Get summary of all import batches (HELP_DESK only)
     */
    @GetMapping("/batches")
    @PreAuthorize("hasAuthority('ROLE_HELP_DESK')")
    public ResponseEntity<List<BankBatchSummaryDto>> getBatchSummaries() {
        List<BankBatchSummaryDto> batches = reconciliationService.getBatchSummaries();
        return ResponseEntity.ok(batches);
    }
}
