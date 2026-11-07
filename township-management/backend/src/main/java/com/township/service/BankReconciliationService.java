package com.township.service;

import com.township.dto.BankBatchSummaryDto;
import com.township.dto.BankTransactionDto;
import com.township.dto.ReconcileApprovalRequest;
import com.township.dto.ReconcileApprovalResponse;
import com.township.entity.*;
import com.township.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BankReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(BankReconciliationService.class);

    private final BankStatementBatchRepository batchRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final MaintenanceBillRepository billRepository;
    private final FlatRepository flatRepository;
    private final FlatLedgerEntryRepository ledgerRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    public BankReconciliationService(
            BankStatementBatchRepository batchRepository,
            BankTransactionRepository bankTransactionRepository,
            MaintenanceBillRepository billRepository,
            FlatRepository flatRepository,
            FlatLedgerEntryRepository ledgerRepository,
            PaymentRepository paymentRepository,
            UserRepository userRepository) {
        this.batchRepository = batchRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.billRepository = billRepository;
        this.flatRepository = flatRepository;
        this.ledgerRepository = ledgerRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
    }

    /**
     * Import and parse a bank statement CSV, validating headers, data types, and preventing duplicate UTRs.
     */
    @Transactional
    public BankBatchSummaryDto importBankStatementCsv(MultipartFile file, String uploadedBy) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded CSV statement file is empty.");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File exceeds maximum allowed limit of 5MB.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("Invalid file format. Only standard .csv statement files are permitted.");
        }

        String batchId = "BATCH-" + DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now())
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        List<Flat> allFlats = flatRepository.findAll();
        List<BankTransaction> parsedTransactions = new ArrayList<>();

        int exactCount = 0;
        int reviewCount = 0;
        int unmatchedCount = 0;
        BigDecimal totalCredit = BigDecimal.ZERO;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new IllegalArgumentException("CSV file contains no header row.");
            }

            // Map header positions
            Map<String, Integer> headerMap = parseHeaders(headerLine);
            if (!headerMap.containsKey("date") || !headerMap.containsKey("amount") || !headerMap.containsKey("utr")) {
                throw new IllegalArgumentException(
                        "CSV statement must contain headers for Date, Amount, and UTR/Reference. Found: " + headerLine);
            }

            int dateIdx = headerMap.get("date");
            int amountIdx = headerMap.get("amount");
            int utrIdx = headerMap.get("utr");
            Integer narrationIdx = headerMap.get("narration");
            Integer upiIdx = headerMap.get("upi");

            // Initialize batch placeholder
            BankStatementBatch batch = new BankStatementBatch(
                    batchId,
                    originalFilename,
                    file.getSize(),
                    0,
                    BigDecimal.ZERO,
                    0,
                    0,
                    0,
                    "PROCESSING",
                    uploadedBy
            );
            batch = batchRepository.save(batch);

            String line;
            int lineNumber = 1;
            int duplicatesSkipped = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue;

                String[] cols = parseCsvLine(line);
                if (cols.length <= Math.max(dateIdx, Math.max(amountIdx, utrIdx))) {
                    continue; // Skip malformed rows
                }

                String dateStr = cols[dateIdx].trim();
                String amountStr = cols[amountIdx].trim().replace(",", "").replace("₹", "").replace("INR", "").trim();
                String utrStr = cols[utrIdx].trim();
                String narration = narrationIdx != null && cols.length > narrationIdx ? cols[narrationIdx].trim() : "Direct Bank Credit";
                String upiId = upiIdx != null && cols.length > upiIdx ? cols[upiIdx].trim() : "";

                if (utrStr.isEmpty()) continue;

                // Prevent duplicate bank transaction imports
                if (bankTransactionRepository.existsByUtrReference(utrStr)) {
                    log.info("Skipping duplicate bank transaction with existing UTR: {}", utrStr);
                    duplicatesSkipped++;
                    continue;
                }

                LocalDate txnDate = parseFlexibleDate(dateStr);
                BigDecimal amount;
                try {
                    amount = new BigDecimal(amountStr);
                } catch (NumberFormatException e) {
                    continue; // Skip debit or non-numeric rows
                }

                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    continue; // Process credits only
                }

                totalCredit = totalCredit.add(amount);

                // Run Deterministic Matching Algorithm
                MatchResult match = evaluateTransactionMatch(amount, utrStr, narration, upiId, allFlats);

                BankTransaction txn = new BankTransaction(
                        batch,
                        txnDate,
                        amount,
                        utrStr,
                        narration,
                        upiId,
                        match.status,
                        match.matchedFlat,
                        match.matchedBill,
                        match.confidenceScore,
                        match.matchReason
                );

                if (match.status == BankMatchStatus.EXACT_MATCH || match.status == BankMatchStatus.RECONCILED) {
                    exactCount++;
                } else if (match.status == BankMatchStatus.MANUAL_REVIEW) {
                    reviewCount++;
                } else {
                    unmatchedCount++;
                }

                parsedTransactions.add(txn);
            }

            bankTransactionRepository.saveAll(parsedTransactions);

            batch.setTotalRows(parsedTransactions.size());
            batch.setTotalCreditAmount(totalCredit);
            batch.setExactMatchesCount(exactCount);
            batch.setManualReviewCount(reviewCount);
            batch.setUnmatchedCount(unmatchedCount);
            batch.setStatus("PROCESSED");
            batch = batchRepository.save(batch);

            return convertToBatchSummary(batch, duplicatesSkipped);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse bank CSV: ", e);
            throw new IllegalArgumentException("Error processing bank statement: " + e.getMessage());
        }
    }

    /**
     * Deterministic Multi-Rule Reconciliation Matching Engine
     */
    private MatchResult evaluateTransactionMatch(
            BigDecimal amount,
            String utr,
            String narration,
            String upiId,
            List<Flat> flats) {

        // Rule 1: Exact UTR already recorded through a payment (Ensure NO double counting!)
        var existingPayment = paymentRepository.findByReferenceNumber(utr);
        if (existingPayment.isPresent()) {
            Payment p = existingPayment.get();
            return new MatchResult(
                    BankMatchStatus.RECONCILED,
                    p.getFlat(),
                    p.getBill(),
                    100,
                    "Verified: Payment already recorded via receipt #" + p.getReceiptNumber()
            );
        }

        // Rule 2: Token match in Narration (e.g. "T07-1203", "T07 1203", "1203") + Exact Outstanding Dues
        String cleanNarration = narration.toUpperCase();
        for (Flat flat : flats) {
            String towerCode = flat.getTower().getId().toUpperCase(); // e.g. T07
            String flatNum = flat.getFlatNumber().toUpperCase();      // e.g. 1203
            String combinedCode = towerCode + "-" + flatNum;           // T07-1203
            String spacedCode = towerCode + " " + flatNum;             // T07 1203

            boolean mentionsFlat = cleanNarration.contains(combinedCode) ||
                    cleanNarration.contains(spacedCode) ||
                    (cleanNarration.contains(towerCode) && cleanNarration.contains(flatNum)) ||
                    cleanNarration.contains("FLAT " + flatNum) ||
                    cleanNarration.contains("UNIT " + flatNum) ||
                    cleanNarration.contains("#" + flatNum);

            if (mentionsFlat) {
                // Check if this flat has an active bill with matching outstanding balance
                List<MaintenanceBill> openBills = billRepository.findByFlatIdOrderByBillingMonthDesc(flat.getId())
                        .stream()
                        .filter(b -> b.getStatus() != BillStatus.PAID && b.getOutstandingBalance().compareTo(BigDecimal.ZERO) > 0)
                        .toList();

                for (MaintenanceBill bill : openBills) {
                    if (bill.getOutstandingBalance().compareTo(amount) == 0) {
                        return new MatchResult(
                                BankMatchStatus.EXACT_MATCH,
                                flat,
                                bill,
                                95,
                                "Deterministic match: Unit " + combinedCode + " with exact outstanding dues of ₹" + amount
                        );
                    }
                }

                // If flat matched but amount doesn't match single bill exactly -> Route to Manual Review
                return new MatchResult(
                        BankMatchStatus.MANUAL_REVIEW,
                        flat,
                        openBills.isEmpty() ? null : openBills.get(0),
                        65,
                        "Identified Unit " + combinedCode + ", but amount ₹" + amount + " requires staff review"
                );
            }
        }

        // Rule 3: UPI ID match to resident
        if (upiId != null && !upiId.isBlank()) {
            String cleanUpi = upiId.trim().toLowerCase();
            for (Flat flat : flats) {
                var members = flatRepository.findById(flat.getId()).map(Flat::getTower).orElse(null);
                // In Phase 5/6, if narration contains UPI resident phone, match candidate
                if (cleanNarration.contains(cleanUpi)) {
                    return new MatchResult(
                            BankMatchStatus.MANUAL_REVIEW,
                            flat,
                            null,
                            50,
                            "Candidate UPI ID match in narration (" + cleanUpi + ")"
                    );
                }
            }
        }

        // Rule 4: Ambiguous Case (Amount equals standard fee, but narration is generic)
        return new MatchResult(
                BankMatchStatus.UNMATCHED,
                null,
                null,
                0,
                "Unmatched: No unit number or UTR found in bank narration"
        );
    }

    /**
     * Manual Match and Approval Workflow (HELP_DESK only).
     * Only after verified matching and authorized approval does a bank payment update a bill and ledger!
     */
    @Transactional
    public ReconcileApprovalResponse reconcileAndApprove(ReconcileApprovalRequest request, String staffEmail) {
        Long txnId = request.resolvedTransactionId();
        if (txnId == null) {
            throw new IllegalArgumentException("Bank transaction ID is required.");
        }

        BankTransaction txn = bankTransactionRepository.findById(txnId)
                .orElseThrow(() -> new IllegalArgumentException("Bank transaction not found with ID: " + txnId));

        if (txn.getMatchStatus() == BankMatchStatus.RECONCILED) {
            throw new IllegalArgumentException("This bank transaction has already been approved and reconciled.");
        }

        if ("REJECT".equalsIgnoreCase(request.action())) {
            txn.setMatchStatus(BankMatchStatus.REJECTED);
            txn.setReconciledAt(LocalDateTime.now());
            txn.setReconciledBy(staffEmail);
            txn.setMatchReason("Rejected by staff: " + (request.notes() != null ? request.notes() : "Unverified transaction"));
            bankTransactionRepository.save(txn);

            return new ReconcileApprovalResponse(
                    txn.getId(),
                    "REJECTED",
                    txn.getAmount(),
                    null,
                    null,
                    null,
                    "Bank transaction rejected. No funds posted to township ledger."
            );
        }

        // Action: APPROVE
        Long billId = request.targetBillId() != null
                ? request.targetBillId()
                : (txn.getMatchedBill() != null ? txn.getMatchedBill().getId() : null);

        MaintenanceBill bill = null;
        if (billId != null) {
            bill = billRepository.findById(billId)
                    .orElseThrow(() -> new IllegalArgumentException("Maintenance bill not found with ID: " + billId));
        } else if (request.targetFlatId() != null) {
            bill = billRepository.findByFlatIdOrderByBillingMonthDesc(request.targetFlatId()).stream()
                    .filter(b -> b.getOutstandingBalance().compareTo(BigDecimal.ZERO) > 0)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No bill with outstanding balance found for flat ID: " + request.targetFlatId()));
        } else if (txn.getMatchedFlat() != null) {
            bill = billRepository.findByFlatIdOrderByBillingMonthDesc(txn.getMatchedFlat().getId()).stream()
                    .filter(b -> b.getOutstandingBalance().compareTo(BigDecimal.ZERO) > 0)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No bill with outstanding balance found for flat: " + txn.getMatchedFlat().getFlatNumber()));
        } else {
            throw new IllegalArgumentException("A target apartment flat or bill must be specified to approve reconciliation.");
        }

        // Prevent double counting: Ensure this UTR is not already in payments
        if (paymentRepository.existsByReferenceNumber(txn.getUtrReference())) {
            throw new IllegalArgumentException("Double counting prevented: UTR " + txn.getUtrReference() + " is already recorded.");
        }

        if (bill.getStatus() == BillStatus.PAID || bill.getOutstandingBalance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Target bill #" + bill.getId() + " is already fully settled.");
        }

        if (txn.getAmount().compareTo(bill.getOutstandingBalance()) > 0) {
            throw new IllegalArgumentException(String.format(
                    "Reconciliation amount ₹%s exceeds bill outstanding balance ₹%s.",
                    txn.getAmount(), bill.getOutstandingBalance()));
        }

        // 1. Update bill
        BigDecimal previousOutstanding = bill.getOutstandingBalance();
        BigDecimal newPaidAmount = bill.getPaidAmount().add(txn.getAmount());
        BigDecimal newOutstanding = previousOutstanding.subtract(txn.getAmount());

        bill.setPaidAmount(newPaidAmount);
        bill.setOutstandingBalance(newOutstanding);
        if (newOutstanding.compareTo(BigDecimal.ZERO) == 0) {
            bill.setStatus(BillStatus.PAID);
        } else {
            bill.setStatus(BillStatus.PARTIALLY_PAID);
        }
        billRepository.save(bill);

        // 2. Record Payment in PostgreSQL
        String receiptNum = "RCP-REC-" + DateTimeFormatter.ofPattern("yyyyMM").format(LocalDate.now())
                + "-" + (int)(Math.random() * 89999 + 10000);

        Payment payment = new Payment(
                bill,
                bill.getFlat(),
                null,
                txn.getAmount(),
                txn.getTransactionDate().atTime(12, 0),
                "BANK_NEFT_RECONCILED",
                txn.getUtrReference(),
                "COMPLETED",
                "Reconciled bank statement transaction via batch #" + txn.getBatch().getBatchId(),
                true,
                receiptNum
        );
        payment = paymentRepository.save(payment);

        // 3. Append credit to Flat Ledger to preserve financial audit trail
        BigDecimal prevRunning = ledgerRepository.findTopByFlatIdOrderByEntryDateDescIdDesc(bill.getFlat().getId())
                .map(FlatLedgerEntry::getRunningBalance)
                .orElse(BigDecimal.ZERO);

        BigDecimal newRunning = prevRunning.subtract(txn.getAmount());

        FlatLedgerEntry ledgerEntry = new FlatLedgerEntry(
                bill.getFlat(),
                bill,
                LocalDateTime.now(),
                LedgerEntryType.PAYMENT,
                "Bank Statement Reconciliation Credit (UTR: " + txn.getUtrReference() + ")",
                BigDecimal.ZERO,
                txn.getAmount(),
                newRunning,
                txn.getUtrReference()
        );
        ledgerRepository.save(ledgerEntry);

        // 4. Update Bank Transaction
        txn.setMatchStatus(BankMatchStatus.RECONCILED);
        txn.setMatchedBill(bill);
        txn.setMatchedFlat(bill.getFlat());
        txn.setPayment(payment);
        txn.setReconciledAt(LocalDateTime.now());
        txn.setReconciledBy(staffEmail);
        txn.setMatchReason("Approved and posted to flat ledger by " + staffEmail);
        bankTransactionRepository.save(txn);

        return new ReconcileApprovalResponse(
                txn.getId(),
                "RECONCILED",
                txn.getAmount(),
                bill.getId(),
                bill.getFlat().getFlatNumber(),
                bill.getStatus(),
                "Successfully approved and credited ₹" + txn.getAmount() + " to Unit #" + bill.getFlat().getFlatNumber()
        );
    }

    @Transactional(readOnly = true)
    public List<BankTransactionDto> getBankTransactions(BankMatchStatus status, String batchIdParam) {
        List<BankTransaction> list = bankTransactionRepository.findAllByOrderByTransactionDateDescCreatedAtDesc();

        return list.stream()
                .filter(t -> status == null || t.getMatchStatus() == status)
                .filter(t -> {
                    if (batchIdParam == null || batchIdParam.isBlank()) return true;
                    String trimmed = batchIdParam.trim();
                    return String.valueOf(t.getBatch().getId()).equals(trimmed)
                            || t.getBatch().getBatchId().equalsIgnoreCase(trimmed);
                })
                .map(this::convertToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BankBatchSummaryDto> getBatchSummaries() {
        return batchRepository.findAllByOrderByImportedAtDesc()
                .stream()
                .map(this::convertToBatchSummary)
                .toList();
    }

    private Map<String, Integer> parseHeaders(String headerLine) {
        Map<String, Integer> map = new HashMap<>();
        String[] headers = parseCsvLine(headerLine);
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i].toLowerCase().trim().replace("\"", "").replace("_", " ");
            if (h.contains("date") || h.contains("txn date") || h.contains("value date")) {
                map.put("date", i);
            } else if (h.contains("amount") || h.contains("credit") || h.contains("deposit")) {
                map.put("amount", i);
            } else if (h.contains("utr") || h.contains("reference") || h.contains("ref no") || h.contains("chq/ref")) {
                map.put("utr", i);
            } else if (h.contains("narration") || h.contains("description") || h.contains("particulars")) {
                map.put("narration", i);
            } else if (h.contains("upi") || h.contains("vpa")) {
                map.put("upi", i);
            }
        }
        return map;
    }

    private String[] parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (char c : line.toCharArray()) {
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        return tokens.toArray(new String[0]);
    }

    private LocalDate parseFlexibleDate(String dateStr) {
        String[] patterns = {"yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy", "yyyy/MM/dd", "d/M/yyyy", "d-M-yyyy"};
        for (String p : patterns) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(p));
            } catch (Exception ignored) {}
        }
        return LocalDate.now();
    }

    public BankTransactionDto convertToDto(BankTransaction t) {
        return new BankTransactionDto(
                t.getId(),
                t.getBatch().getBatchId(),
                t.getTransactionDate(),
                t.getAmount(),
                t.getUtrReference(),
                t.getNarration(),
                t.getUpiId(),
                t.getMatchStatus(),
                t.getMatchedFlat() != null ? t.getMatchedFlat().getId() : null,
                t.getMatchedFlat() != null ? t.getMatchedFlat().getTower().getName() : null,
                t.getMatchedFlat() != null ? t.getMatchedFlat().getFlatNumber() : null,
                t.getMatchedBill() != null ? t.getMatchedBill().getId() : null,
                t.getMatchedBill() != null ? t.getMatchedBill().getBillingMonth() : null,
                t.getConfidenceScore(),
                t.getMatchReason(),
                t.getReconciledAt(),
                t.getReconciledBy(),
                t.getPayment() != null ? t.getPayment().getId() : null
        );
    }

    public BankBatchSummaryDto convertToBatchSummary(BankStatementBatch b) {
        return convertToBatchSummary(b, 0);
    }

    public BankBatchSummaryDto convertToBatchSummary(BankStatementBatch b, int duplicatesSkipped) {
        return new BankBatchSummaryDto(
                b.getId(),
                b.getBatchId(),
                b.getFilename(),
                b.getFileSizeBytes(),
                b.getTotalRows(),
                b.getTotalCreditAmount(),
                b.getExactMatchesCount(),
                b.getManualReviewCount(),
                b.getUnmatchedCount(),
                duplicatesSkipped,
                b.getStatus(),
                b.getUploadedBy(),
                b.getImportedAt()
        );
    }

    private static class MatchResult {
        BankMatchStatus status;
        Flat matchedFlat;
        MaintenanceBill matchedBill;
        int confidenceScore;
        String matchReason;

        MatchResult(BankMatchStatus status, Flat matchedFlat, MaintenanceBill matchedBill, int confidenceScore, String matchReason) {
            this.status = status;
            this.matchedFlat = matchedFlat;
            this.matchedBill = matchedBill;
            this.confidenceScore = confidenceScore;
            this.matchReason = matchReason;
        }
    }
}
