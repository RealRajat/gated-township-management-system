package com.township.service;

import com.township.dto.PaymentHistoryItemDto;
import com.township.dto.PaymentReceiptDto;
import com.township.dto.TestPaymentRequest;
import com.township.entity.*;
import com.township.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private final MaintenanceBillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final FlatLedgerEntryRepository ledgerRepository;
    private final FlatMemberRepository flatMemberRepository;
    private final UserRepository userRepository;

    public PaymentService(
            MaintenanceBillRepository billRepository,
            PaymentRepository paymentRepository,
            FlatLedgerEntryRepository ledgerRepository,
            FlatMemberRepository flatMemberRepository,
            UserRepository userRepository) {
        this.billRepository = billRepository;
        this.paymentRepository = paymentRepository;
        this.ledgerRepository = ledgerRepository;
        this.flatMemberRepository = flatMemberRepository;
        this.userRepository = userRepository;
    }

    /**
     * Process a test payment with transactional integrity, idempotency, and overpayment prevention.
     */
    @Transactional
    public PaymentReceiptDto processTestPayment(TestPaymentRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);

        // 1. Retrieve bill
        MaintenanceBill bill = billRepository.findById(request.billId())
                .orElseThrow(() -> new IllegalArgumentException("Bill not found with ID: " + request.billId()));

        // 2. Validate resident data isolation: resident can only pay their own flat's bill
        validateFlatAccess(bill.getFlat().getId(), user);

        // 3. Check bill status
        if (bill.getStatus() == BillStatus.PAID || bill.getOutstandingBalance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("This bill is already settled in full. No outstanding balance remains.");
        }

        // 4. Overpayment prevention & Amount validation
        BigDecimal amount = request.amount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be strictly greater than zero.");
        }

        if (amount.compareTo(bill.getOutstandingBalance()) > 0) {
            throw new IllegalArgumentException(String.format(
                    "Overpayment rejected: Attempted to pay ₹%s, but outstanding balance is only ₹%s.",
                    amount, bill.getOutstandingBalance()));
        }

        // 5. Idempotency Check
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            var existingPayment = paymentRepository.findByReferenceNumber(request.idempotencyKey().trim());
            if (existingPayment.isPresent()) {
                return convertToReceipt(existingPayment.get());
            }
        }

        // 6. Test Mode Simulation: Failure Injection Support
        if (Boolean.TRUE.equals(request.simulateFailure())) {
            String reason = request.failureReason() != null && !request.failureReason().isBlank()
                    ? request.failureReason().trim()
                    : "Simulated test bank gateway decline: Insufficient balance or test network error";
            throw new IllegalArgumentException(reason);
        }

        // 7. Generate Unique Test Transaction Reference & Receipt Number
        String txnRef = request.idempotencyKey() != null && !request.idempotencyKey().isBlank()
                ? request.idempotencyKey().trim()
                : "TEST-TXN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        String receiptNum = "RCP-" + DateTimeFormatter.ofPattern("yyyyMM").format(LocalDateTime.now())
                + "-" + (int)(Math.random() * 89999 + 10000);

        BigDecimal previousOutstanding = bill.getOutstandingBalance();
        BigDecimal newPaidAmount = bill.getPaidAmount().add(amount);
        BigDecimal remainingBalance = previousOutstanding.subtract(amount);

        // 8. Update Bill State
        bill.setPaidAmount(newPaidAmount);
        bill.setOutstandingBalance(remainingBalance);
        if (remainingBalance.compareTo(BigDecimal.ZERO) == 0) {
            bill.setStatus(BillStatus.PAID);
        } else {
            bill.setStatus(BillStatus.PARTIALLY_PAID);
        }
        billRepository.save(bill);

        // 9. Record Payment in PostgreSQL
        Payment payment = new Payment(
                bill,
                bill.getFlat(),
                user,
                amount,
                LocalDateTime.now(),
                request.paymentMethod() != null ? request.paymentMethod().trim() : "UPI_TEST_MODE",
                txnRef,
                "COMPLETED",
                "TEST MODE settlement for " + bill.getBillingMonth(),
                true,
                receiptNum
        );
        payment = paymentRepository.save(payment);

        // 10. Append Immutable Credit Entry to Flat Ledger
        BigDecimal prevRunningBalance = ledgerRepository.findTopByFlatIdOrderByEntryDateDescIdDesc(bill.getFlat().getId())
                .map(FlatLedgerEntry::getRunningBalance)
                .orElse(BigDecimal.ZERO);

        BigDecimal newRunningBalance = prevRunningBalance.subtract(amount);

        FlatLedgerEntry ledgerEntry = new FlatLedgerEntry(
                bill.getFlat(),
                bill,
                LocalDateTime.now(),
                LedgerEntryType.PAYMENT,
                "Test Payment Received - " + bill.getBillingMonth() + " (Ref: " + txnRef + ")",
                BigDecimal.ZERO,
                amount,
                newRunningBalance,
                txnRef
        );
        ledgerRepository.save(ledgerEntry);

        return convertToReceipt(payment);
    }

    /**
     * Get resident payment history
     */
    @Transactional(readOnly = true)
    public List<PaymentHistoryItemDto> getResidentPaymentHistory(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<FlatMember> memberships = flatMemberRepository.findByUserId(user.getId());
        if (memberships.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> flatIds = memberships.stream().map(m -> m.getFlat().getId()).toList();
        List<Payment> payments = paymentRepository.findByFlatIdInOrderByPaymentDateDesc(flatIds);

        return payments.stream().map(this::convertToHistoryItem).toList();
    }

    /**
     * Help Desk: view all payments across township
     */
    @Transactional(readOnly = true)
    public List<PaymentHistoryItemDto> getAllPayments(String towerId, String paymentMethod) {
        List<Payment> payments = paymentRepository.findAllByOrderByPaymentDateDesc();

        return payments.stream()
                .filter(p -> towerId == null || towerId.isBlank() || p.getFlat().getTower().getId().equalsIgnoreCase(towerId.trim()))
                .filter(p -> paymentMethod == null || paymentMethod.isBlank() || p.getPaymentMethod().equalsIgnoreCase(paymentMethod.trim()))
                .map(this::convertToHistoryItem)
                .toList();
    }

    /**
     * Get single receipt by payment ID
     */
    @Transactional(readOnly = true)
    public PaymentReceiptDto getPaymentReceipt(Long paymentId, String userEmail) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment record not found with ID: " + paymentId));

        User user = getUserByEmail(userEmail);
        validateFlatAccess(payment.getFlat().getId(), user);

        return convertToReceipt(payment);
    }

    private void validateFlatAccess(Long flatId, User user) {
        if (user.getRole() == Role.HELP_DESK) {
            return; // Staff has township-wide access
        }
        boolean hasAccess = flatMemberRepository.existsByFlatIdAndUserId(flatId, user.getId());
        if (!hasAccess) {
            throw new AccessDeniedException("Access denied: You are not authorized to make payments or view receipts for flat ID: " + flatId);
        }
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
    }

    public PaymentReceiptDto convertToReceipt(Payment payment) {
        MaintenanceBill bill = payment.getBill();
        Flat flat = payment.getFlat();
        User resident = payment.getUser();

        BigDecimal previousOutstanding = bill.getOutstandingBalance().add(payment.getAmount());

        String txnRef = payment.getReferenceNumber();
        String notice = "TEST MODE SIMULATION - APEX FACILITY MANAGEMENT SERVICES PVT. LTD.";

        return new PaymentReceiptDto(
                payment.getId(),
                payment.getReceiptNumber() != null ? payment.getReceiptNumber() : "RCP-" + payment.getId(),
                txnRef,
                txnRef,
                bill.getId(),
                bill.getBillingMonth(),
                flat.getId(),
                flat.getTower().getId(),
                flat.getTower().getName(),
                flat.getFlatNumber(),
                resident != null ? resident.getFullName() : "Resident Owner",
                resident != null ? resident.getEmail() : "N/A",
                payment.getAmount(),
                payment.getPaymentDate(),
                payment.getPaymentMethod(),
                previousOutstanding,
                bill.getOutstandingBalance(),
                bill.getStatus(),
                payment.isTestMode(),
                notice,
                notice
        );
    }

    public PaymentHistoryItemDto convertToHistoryItem(Payment p) {
        return new PaymentHistoryItemDto(
                p.getId(),
                p.getReceiptNumber() != null ? p.getReceiptNumber() : "RCP-" + p.getId(),
                p.getReferenceNumber(),
                p.getBill().getId(),
                p.getBill().getBillingMonth(),
                p.getFlat().getId(),
                p.getFlat().getTower().getName(),
                p.getFlat().getFlatNumber(),
                p.getUser() != null ? p.getUser().getFullName() : "Registered Resident",
                p.getAmount(),
                p.getPaymentDate(),
                p.getPaymentMethod(),
                p.getStatus(),
                p.isTestMode()
        );
    }
}
