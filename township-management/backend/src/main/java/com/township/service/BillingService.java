package com.township.service;

import com.township.dto.*;
import com.township.entity.*;
import com.township.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

@Service
public class BillingService {

    private final MaintenanceBillRepository billRepository;
    private final FlatLedgerEntryRepository ledgerRepository;
    private final BillingConfigurationRepository configRepository;
    private final FlatRepository flatRepository;
    private final FlatMemberRepository flatMemberRepository;
    private final UserRepository userRepository;

    public BillingService(
            MaintenanceBillRepository billRepository,
            FlatLedgerEntryRepository ledgerRepository,
            BillingConfigurationRepository configRepository,
            FlatRepository flatRepository,
            FlatMemberRepository flatMemberRepository,
            UserRepository userRepository) {
        this.billRepository = billRepository;
        this.ledgerRepository = ledgerRepository;
        this.configRepository = configRepository;
        this.flatRepository = flatRepository;
        this.flatMemberRepository = flatMemberRepository;
        this.userRepository = userRepository;
    }

    /**
     * Get active billing configuration or default
     */
    @Transactional(readOnly = true)
    public BillingConfiguration getActiveConfig() {
        return configRepository.findFirstByActiveTrueOrderByUpdatedAtDesc()
                .orElseGet(() -> new BillingConfiguration(
                        new BigDecimal("6.50"),
                        15,
                        "PERCENTAGE",
                        new BigDecimal("10.00"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        true
                ));
    }

    /**
     * Resident billing summary with data isolation check
     */
    @Transactional(readOnly = true)
    public ResidentBillingSummaryDto getResidentBillingSummary(String userEmail) {
        User user = getUserByEmail(userEmail);

        List<FlatMember> memberships = flatMemberRepository.findByUserId(user.getId());
        if (memberships.isEmpty()) {
            return new ResidentBillingSummaryDto(
                    Collections.emptyList(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    0,
                    Collections.emptyList()
            );
        }

        List<Long> flatIds = memberships.stream().map(m -> m.getFlat().getId()).toList();
        List<MaintenanceBill> bills = billRepository.findByFlatIdInOrderByBillingMonthDesc(flatIds);

        List<FlatSummaryDto> flatSummaries = memberships.stream()
                .map(m -> new FlatSummaryDto(
                        m.getFlat().getId(),
                        m.getFlat().getTower().getId(),
                        m.getFlat().getTower().getName(),
                        m.getFlat().getFlatNumber(),
                        m.getFlat().getFloorNumber(),
                        m.getFlat().getAreaSqFt(),
                        m.getFlat().getOccupancyStatus(),
                        m.getMemberType()
                ))
                .toList();

        BigDecimal currentMonthlyDues = bills.stream()
                .filter(b -> b.getStatus() == BillStatus.UNPAID || b.getStatus() == BillStatus.PARTIALLY_PAID || b.getStatus() == BillStatus.OVERDUE)
                .map(MaintenanceBill::getOutstandingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = bills.stream()
                .map(MaintenanceBill::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOutstanding = bills.stream()
                .map(MaintenanceBill::getOutstandingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long overdueCount = bills.stream()
                .filter(b -> b.getStatus() == BillStatus.OVERDUE)
                .count();

        List<MaintenanceBillDto> billDtos = bills.stream().map(this::convertToBillDto).toList();

        return new ResidentBillingSummaryDto(
                flatSummaries,
                currentMonthlyDues,
                totalPaid,
                totalOutstanding,
                overdueCount,
                billDtos
        );
    }

    /**
     * Get single bill with resident data isolation check
     */
    @Transactional(readOnly = true)
    public MaintenanceBillDto getBillById(Long billId, String userEmail) {
        MaintenanceBill bill = billRepository.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found with ID: " + billId));

        validateFlatAccess(bill.getFlat().getId(), userEmail);

        return convertToBillDto(bill);
    }

    /**
     * Get flat ledger history with resident data isolation check
     */
    @Transactional(readOnly = true)
    public List<FlatLedgerEntryDto> getFlatLedger(Long flatId, String userEmail) {
        Flat flat = flatRepository.findById(flatId)
                .orElseThrow(() -> new IllegalArgumentException("Flat not found with ID: " + flatId));

        validateFlatAccess(flat.getId(), userEmail);

        List<FlatLedgerEntry> entries = ledgerRepository.findByFlatIdOrderByEntryDateAscIdAsc(flatId);
        return entries.stream().map(this::convertToLedgerDto).toList();
    }

    /**
     * Help Desk / Admin township-wide billing overview metrics
     */
    @Transactional(readOnly = true)
    public BillingOverviewDto getBillingOverview() {
        BigDecimal totalBilled = billRepository.sumTotalDue();
        BigDecimal totalCollected = billRepository.sumPaidAmount();
        BigDecimal totalOutstanding = billRepository.sumOutstandingBalance();

        double collectionRate = 0.0;
        if (totalBilled.compareTo(BigDecimal.ZERO) > 0) {
            collectionRate = totalCollected.multiply(BigDecimal.valueOf(100))
                    .divide(totalBilled, 2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        long totalCount = billRepository.count();
        long unpaid = billRepository.countByStatus(BillStatus.UNPAID);
        long partiallyPaid = billRepository.countByStatus(BillStatus.PARTIALLY_PAID);
        long paid = billRepository.countByStatus(BillStatus.PAID);
        long overdue = billRepository.countByStatus(BillStatus.OVERDUE);

        BillingConfiguration config = getActiveConfig();

        return new BillingOverviewDto(
                totalBilled,
                totalCollected,
                totalOutstanding,
                collectionRate,
                totalCount,
                unpaid,
                partiallyPaid,
                paid,
                overdue,
                config.getBaseRatePerSqFt()
        );
    }

    /**
     * Help Desk / Admin list all bills across township with filters
     */
    @Transactional(readOnly = true)
    public List<MaintenanceBillDto> getAllBills(String billingMonth, BillStatus status, String towerId) {
        List<MaintenanceBill> bills = billRepository.findAllByOrderByBillingMonthDescCreatedAtDesc();

        return bills.stream()
                .filter(b -> billingMonth == null || billingMonth.isBlank() || b.getBillingMonth().equalsIgnoreCase(billingMonth.trim()))
                .filter(b -> status == null || b.getStatus() == status)
                .filter(b -> towerId == null || towerId.isBlank() || b.getFlat().getTower().getId().equalsIgnoreCase(towerId.trim()))
                .map(this::convertToBillDto)
                .toList();
    }

    /**
     * Idempotent monthly billing generation across all flats
     */
    @Transactional
    public GenerateBillsResponse generateMonthlyBills(GenerateBillsRequest request) {
        String month = request.billingMonth().trim();
        BillingConfiguration config = getActiveConfig();
        BigDecimal rate = config.getBaseRatePerSqFt();

        int dueDays = request.dueDays() != null && request.dueDays() > 0 ? request.dueDays() : config.getGracePeriodDays();

        LocalDate issueDate = LocalDate.now();
        LocalDate dueDate = issueDate.plusDays(dueDays);

        List<Flat> flats = flatRepository.findAll();
        int generatedCount = 0;
        int skippedCount = 0;
        BigDecimal totalBilled = BigDecimal.ZERO;

        for (Flat flat : flats) {
            // Idempotent duplicate check
            if (billRepository.existsByFlatIdAndBillingMonth(flat.getId(), month)) {
                skippedCount++;
                continue;
            }

            BigDecimal area = BigDecimal.valueOf(flat.getAreaSqFt()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal baseAmount = area.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalDue = baseAmount;
            BigDecimal outstanding = totalDue;

            MaintenanceBill bill = new MaintenanceBill(
                    flat,
                    month,
                    area,
                    rate,
                    baseAmount,
                    BigDecimal.ZERO,
                    totalDue,
                    BigDecimal.ZERO,
                    outstanding,
                    BillStatus.UNPAID,
                    issueDate,
                    dueDate,
                    request.notes() != null && !request.notes().isBlank()
                            ? request.notes().trim()
                            : "Standard Monthly Maintenance Bill for " + month + " (Rate: ₹" + rate + "/sq.ft)"
            );
            bill = billRepository.save(bill);

            // Append to Flat Ledger Entry to preserve financial audit trail
            BigDecimal prevBalance = ledgerRepository.findTopByFlatIdOrderByEntryDateDescIdDesc(flat.getId())
                    .map(FlatLedgerEntry::getRunningBalance)
                    .orElse(BigDecimal.ZERO);

            BigDecimal newBalance = prevBalance.add(totalDue);

            FlatLedgerEntry ledgerEntry = new FlatLedgerEntry(
                    flat,
                    bill,
                    LocalDateTime.now(),
                    LedgerEntryType.BILL_GENERATION,
                    "Monthly Maintenance Bill - " + month + " (" + area + " sq.ft @ ₹" + rate + ")",
                    totalDue,
                    BigDecimal.ZERO,
                    newBalance,
                    "GEN-" + month.replace("-", "") + "-" + flat.getTower().getId() + "-" + flat.getFlatNumber()
            );
            ledgerRepository.save(ledgerEntry);

            generatedCount++;
            totalBilled = totalBilled.add(totalDue);
        }

        String msg = String.format("Generated %d bills for %s (₹%s total billed). %d existing bills skipped.",
                generatedCount, month, totalBilled, skippedCount);

        return new GenerateBillsResponse(month, generatedCount, skippedCount, totalBilled, msg);
    }

    /**
     * Get active billing config DTO
     */
    @Transactional(readOnly = true)
    public BillingConfigDto getBillingConfig() {
        BillingConfiguration config = getActiveConfig();
        return convertToConfigDto(config);
    }

    /**
     * Update billing config (Help Desk only)
     */
    @Transactional
    public BillingConfigDto updateBillingConfig(BillingConfigDto dto) {
        BillingConfiguration config = configRepository.findFirstByActiveTrueOrderByUpdatedAtDesc()
                .orElseGet(BillingConfiguration::new);

        if (dto.baseRatePerSqFt() != null) config.setBaseRatePerSqFt(dto.baseRatePerSqFt());
        if (dto.gracePeriodDays() != null) config.setGracePeriodDays(dto.gracePeriodDays());
        if (dto.lateFeeType() != null) config.setLateFeeType(dto.lateFeeType());
        if (dto.lateFeePercentage() != null) config.setLateFeePercentage(dto.lateFeePercentage());
        if (dto.lateFeeFixedAmount() != null) config.setLateFeeFixedAmount(dto.lateFeeFixedAmount());
        if (dto.sinkingFundPerFlat() != null) config.setSinkingFundPerFlat(dto.sinkingFundPerFlat());
        config.setActive(true);

        config = configRepository.save(config);
        return convertToConfigDto(config);
    }

    /**
     * Security check: ensure residents can access ONLY flats they are linked to.
     * HELP_DESK users have administrative oversight across all flats.
     */
    public void validateFlatAccess(Long flatId, String userEmail) {
        User user = getUserByEmail(userEmail);
        if (user.getRole() == Role.HELP_DESK) {
            return; // Staff has full access
        }

        boolean hasAccess = flatMemberRepository.existsByFlatIdAndUserId(flatId, user.getId());
        if (!hasAccess) {
            throw new AccessDeniedException("Access denied: You are not authorized to access records for flat ID: " + flatId);
        }
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
    }

    public MaintenanceBillDto convertToBillDto(MaintenanceBill bill) {
        return new MaintenanceBillDto(
                bill.getId(),
                bill.getFlat().getId(),
                bill.getFlat().getTower().getId(),
                bill.getFlat().getTower().getName(),
                bill.getFlat().getFlatNumber(),
                bill.getBillingMonth(),
                bill.getFlatAreaSqFt(),
                bill.getRatePerSqFt(),
                bill.getBaseAmount(),
                bill.getLateFee(),
                bill.getTotalDue(),
                bill.getPaidAmount(),
                bill.getOutstandingBalance(),
                bill.getStatus(),
                bill.getIssueDate(),
                bill.getDueDate(),
                bill.getNotes()
        );
    }

    public FlatLedgerEntryDto convertToLedgerDto(FlatLedgerEntry entry) {
        return new FlatLedgerEntryDto(
                entry.getId(),
                entry.getFlat().getId(),
                entry.getFlat().getTower().getId(),
                entry.getFlat().getTower().getName(),
                entry.getFlat().getFlatNumber(),
                entry.getBill() != null ? entry.getBill().getId() : null,
                entry.getEntryDate(),
                entry.getEntryType(),
                entry.getDescription(),
                entry.getDebit(),
                entry.getCredit(),
                entry.getRunningBalance(),
                entry.getTransactionReference()
        );
    }

    public BillingConfigDto convertToConfigDto(BillingConfiguration config) {
        return new BillingConfigDto(
                config.getId(),
                config.getBaseRatePerSqFt(),
                config.getGracePeriodDays(),
                config.getLateFeeType(),
                config.getLateFeePercentage(),
                config.getLateFeeFixedAmount(),
                config.getSinkingFundPerFlat(),
                config.isActive()
        );
    }
}

