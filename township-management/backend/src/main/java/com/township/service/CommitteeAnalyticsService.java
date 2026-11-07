package com.township.service;

import com.township.dto.*;
import com.township.entity.*;
import com.township.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CommitteeAnalyticsService {

    private final MaintenanceBillRepository billRepository;
    private final ComplaintRepository complaintRepository;
    private final ComplaintActivityRepository complaintActivityRepository;
    private final VisitorRepository visitorRepository;
    private final AmenityBookingRepository amenityBookingRepository;
    private final AmenityRepository amenityRepository;
    private final NoticeRepository noticeRepository;
    private final NoticeReadRepository noticeReadRepository;
    private final TowerRepository towerRepository;
    private final FlatRepository flatRepository;
    private final PaymentRepository paymentRepository;

    public CommitteeAnalyticsService(
            MaintenanceBillRepository billRepository,
            ComplaintRepository complaintRepository,
            ComplaintActivityRepository complaintActivityRepository,
            VisitorRepository visitorRepository,
            AmenityBookingRepository amenityBookingRepository,
            AmenityRepository amenityRepository,
            NoticeRepository noticeRepository,
            NoticeReadRepository noticeReadRepository,
            TowerRepository towerRepository,
            FlatRepository flatRepository,
            PaymentRepository paymentRepository) {
        this.billRepository = billRepository;
        this.complaintRepository = complaintRepository;
        this.complaintActivityRepository = complaintActivityRepository;
        this.visitorRepository = visitorRepository;
        this.amenityBookingRepository = amenityBookingRepository;
        this.amenityRepository = amenityRepository;
        this.noticeRepository = noticeRepository;
        this.noticeReadRepository = noticeReadRepository;
        this.towerRepository = towerRepository;
        this.flatRepository = flatRepository;
        this.paymentRepository = paymentRepository;
    }

    // =========================================================================
    // 1. Full Dashboard Aggregation
    // =========================================================================
    public CommitteeDashboardDto getDashboard(LocalDate fromDate, LocalDate toDate, String towerId) {
        CommitteeFinancialKpiDto financial = getFinancialKpis(fromDate, toDate, towerId);
        List<CollectionTrendDto> collectionTrends = getCollectionTrends(towerId);
        List<DuesAgeingBucketDto> duesAgeing = getDuesAgeing(towerId);
        List<TowerDuesSummaryDto> towerDues = getTowerDuesSummaries();
        ComplaintAnalyticsDto complaints = getComplaintAnalytics(fromDate, toDate, towerId);
        OperationalAnalyticsDto operational = getOperationalAnalytics(fromDate, toDate, towerId);

        return new CommitteeDashboardDto(
                financial,
                collectionTrends,
                duesAgeing,
                towerDues,
                complaints,
                operational
        );
    }

    // =========================================================================
    // 2. Financial KPIs
    // =========================================================================
    public CommitteeFinancialKpiDto getFinancialKpis(LocalDate fromDate, LocalDate toDate, String towerId) {
        List<MaintenanceBill> bills = filterBills(fromDate, toDate, towerId);
        LocalDate today = LocalDate.now();

        BigDecimal totalBilled = BigDecimal.ZERO;
        BigDecimal totalCollected = BigDecimal.ZERO;
        BigDecimal totalOutstanding = BigDecimal.ZERO;

        long paidCount = 0;
        long partiallyPaidCount = 0;
        BigDecimal partiallyPaidAmount = BigDecimal.ZERO;
        long unpaidCount = 0;
        long overdueCount = 0;
        BigDecimal overdueAmount = BigDecimal.ZERO;

        for (MaintenanceBill bill : bills) {
            BigDecimal due = bill.getTotalDue() != null ? bill.getTotalDue() : BigDecimal.ZERO;
            BigDecimal paid = bill.getPaidAmount() != null ? bill.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal balance = bill.getOutstandingBalance() != null ? bill.getOutstandingBalance() : BigDecimal.ZERO;

            totalBilled = totalBilled.add(due);
            totalCollected = totalCollected.add(paid);
            totalOutstanding = totalOutstanding.add(balance);

            if (bill.getStatus() == BillStatus.PAID) {
                paidCount++;
            } else if (bill.getStatus() == BillStatus.PARTIALLY_PAID) {
                partiallyPaidCount++;
                partiallyPaidAmount = partiallyPaidAmount.add(balance);
            } else if (bill.getStatus() == BillStatus.UNPAID) {
                unpaidCount++;
            }

            boolean isOverdue = bill.getStatus() == BillStatus.OVERDUE ||
                    (balance.compareTo(BigDecimal.ZERO) > 0 && bill.getDueDate() != null && today.isAfter(bill.getDueDate()));

            if (isOverdue) {
                overdueCount++;
                overdueAmount = overdueAmount.add(balance);
            }
        }

        double collectionPercentage = 0.0;
        if (totalBilled.compareTo(BigDecimal.ZERO) > 0) {
            collectionPercentage = totalCollected.multiply(BigDecimal.valueOf(100))
                    .divide(totalBilled, 1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return new CommitteeFinancialKpiDto(
                totalBilled,
                totalCollected,
                totalOutstanding,
                collectionPercentage,
                bills.size(),
                paidCount,
                partiallyPaidCount,
                partiallyPaidAmount,
                unpaidCount,
                overdueCount,
                overdueAmount
        );
    }

    // =========================================================================
    // 3. Month-wise Collection Trends
    // =========================================================================
    public List<CollectionTrendDto> getCollectionTrends(String towerId) {
        List<MaintenanceBill> bills = billRepository.findAll();
        if (towerId != null && !towerId.equalsIgnoreCase("ALL") && !towerId.isBlank()) {
            bills = bills.stream()
                    .filter(b -> b.getFlat() != null && b.getFlat().getTower() != null &&
                            b.getFlat().getTower().getId().equalsIgnoreCase(towerId))
                    .toList();
        }

        Map<String, List<MaintenanceBill>> monthMap = bills.stream()
                .filter(b -> b.getBillingMonth() != null)
                .collect(Collectors.groupingBy(MaintenanceBill::getBillingMonth, TreeMap::new, Collectors.toList()));

        List<CollectionTrendDto> trends = new ArrayList<>();
        for (Map.Entry<String, List<MaintenanceBill>> entry : monthMap.entrySet()) {
            String month = entry.getKey();
            BigDecimal billed = BigDecimal.ZERO;
            BigDecimal collected = BigDecimal.ZERO;
            BigDecimal outstanding = BigDecimal.ZERO;

            for (MaintenanceBill b : entry.getValue()) {
                if (b.getTotalDue() != null) billed = billed.add(b.getTotalDue());
                if (b.getPaidAmount() != null) collected = collected.add(b.getPaidAmount());
                if (b.getOutstandingBalance() != null) outstanding = outstanding.add(b.getOutstandingBalance());
            }

            double rate = 0.0;
            if (billed.compareTo(BigDecimal.ZERO) > 0) {
                rate = collected.multiply(BigDecimal.valueOf(100))
                        .divide(billed, 1, RoundingMode.HALF_UP)
                        .doubleValue();
            }

            trends.add(new CollectionTrendDto(month, billed, collected, outstanding, rate));
        }

        return trends;
    }

    // =========================================================================
    // 4. Dues Ageing Analysis
    // =========================================================================
    public List<DuesAgeingBucketDto> getDuesAgeing(String towerId) {
        List<MaintenanceBill> bills = billRepository.findAll();
        if (towerId != null && !towerId.equalsIgnoreCase("ALL") && !towerId.isBlank()) {
            bills = bills.stream()
                    .filter(b -> b.getFlat() != null && b.getFlat().getTower() != null &&
                            b.getFlat().getTower().getId().equalsIgnoreCase(towerId))
                    .toList();
        }

        LocalDate today = LocalDate.now();
        long count0_30 = 0;
        BigDecimal sum0_30 = BigDecimal.ZERO;

        long count31_60 = 0;
        BigDecimal sum31_60 = BigDecimal.ZERO;

        long count61_90 = 0;
        BigDecimal sum61_90 = BigDecimal.ZERO;

        long countAbove90 = 0;
        BigDecimal sumAbove90 = BigDecimal.ZERO;

        BigDecimal totalOutstanding = BigDecimal.ZERO;

        for (MaintenanceBill bill : bills) {
            BigDecimal bal = bill.getOutstandingBalance() != null ? bill.getOutstandingBalance() : BigDecimal.ZERO;
            if (bal.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            totalOutstanding = totalOutstanding.add(bal);
            long days = 0;
            if (bill.getDueDate() != null && today.isAfter(bill.getDueDate())) {
                days = ChronoUnit.DAYS.between(bill.getDueDate(), today);
            }

            if (days <= 30) {
                count0_30++;
                sum0_30 = sum0_30.add(bal);
            } else if (days <= 60) {
                count31_60++;
                sum31_60 = sum31_60.add(bal);
            } else if (days <= 90) {
                count61_90++;
                sum61_90 = sum61_90.add(bal);
            } else {
                countAbove90++;
                sumAbove90 = sumAbove90.add(bal);
            }
        }

        List<DuesAgeingBucketDto> result = new ArrayList<>();
        result.add(createBucket("0–30 days", count0_30, sum0_30, totalOutstanding));
        result.add(createBucket("31–60 days", count31_60, sum31_60, totalOutstanding));
        result.add(createBucket("61–90 days", count61_90, sum61_90, totalOutstanding));
        result.add(createBucket("Above 90 days", countAbove90, sumAbove90, totalOutstanding));

        return result;
    }

    private DuesAgeingBucketDto createBucket(String label, long count, BigDecimal sum, BigDecimal total) {
        double pct = 0.0;
        if (total.compareTo(BigDecimal.ZERO) > 0 && sum.compareTo(BigDecimal.ZERO) > 0) {
            pct = sum.multiply(BigDecimal.valueOf(100))
                    .divide(total, 1, RoundingMode.HALF_UP)
                    .doubleValue();
        }
        return new DuesAgeingBucketDto(label, count, sum, pct);
    }

    // =========================================================================
    // 5. Tower-wise Dues Summary
    // =========================================================================
    public List<TowerDuesSummaryDto> getTowerDuesSummaries() {
        List<Tower> towers = towerRepository.findAllByOrderByIdAsc();
        List<MaintenanceBill> allBills = billRepository.findAll();
        List<Flat> allFlats = flatRepository.findAll();

        Map<String, List<MaintenanceBill>> towerBillMap = allBills.stream()
                .filter(b -> b.getFlat() != null && b.getFlat().getTower() != null)
                .collect(Collectors.groupingBy(b -> b.getFlat().getTower().getId()));

        Map<String, List<Flat>> towerFlatMap = allFlats.stream()
                .filter(f -> f.getTower() != null)
                .collect(Collectors.groupingBy(f -> f.getTower().getId()));

        List<TowerDuesSummaryDto> summaries = new ArrayList<>();
        for (Tower tower : towers) {
            List<MaintenanceBill> bills = towerBillMap.getOrDefault(tower.getId(), Collections.emptyList());
            List<Flat> flats = towerFlatMap.getOrDefault(tower.getId(), Collections.emptyList());

            BigDecimal billed = BigDecimal.ZERO;
            BigDecimal collected = BigDecimal.ZERO;
            BigDecimal outstanding = BigDecimal.ZERO;
            Set<Long> defaulterFlatIds = new HashSet<>();

            for (MaintenanceBill b : bills) {
                if (b.getTotalDue() != null) billed = billed.add(b.getTotalDue());
                if (b.getPaidAmount() != null) collected = collected.add(b.getPaidAmount());
                if (b.getOutstandingBalance() != null && b.getOutstandingBalance().compareTo(BigDecimal.ZERO) > 0) {
                    outstanding = outstanding.add(b.getOutstandingBalance());
                    if (b.getFlat() != null) {
                        defaulterFlatIds.add(b.getFlat().getId());
                    }
                }
            }

            double collectionRate = 0.0;
            if (billed.compareTo(BigDecimal.ZERO) > 0) {
                collectionRate = collected.multiply(BigDecimal.valueOf(100))
                        .divide(billed, 1, RoundingMode.HALF_UP)
                        .doubleValue();
            }

            summaries.add(new TowerDuesSummaryDto(
                    tower.getId(),
                    tower.getName(),
                    flats.size(),
                    billed,
                    collected,
                    outstanding,
                    collectionRate,
                    defaulterFlatIds.size()
            ));
        }

        return summaries;
    }

    // =========================================================================
    // 6. Defaulter Summary (Privacy-safe: no plain phone/passwords)
    // =========================================================================
    public List<DefaulterSummaryDto> getDefaulterSummaries(String towerId) {
        List<MaintenanceBill> bills = billRepository.findAll();
        if (towerId != null && !towerId.equalsIgnoreCase("ALL") && !towerId.isBlank()) {
            bills = bills.stream()
                    .filter(b -> b.getFlat() != null && b.getFlat().getTower() != null &&
                            b.getFlat().getTower().getId().equalsIgnoreCase(towerId))
                    .toList();
        }

        LocalDate today = LocalDate.now();

        // Group unpaid/partially paid bills by flat
        Map<Long, List<MaintenanceBill>> flatBillsMap = bills.stream()
                .filter(b -> b.getFlat() != null && b.getOutstandingBalance() != null &&
                        b.getOutstandingBalance().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.groupingBy(b -> b.getFlat().getId()));

        List<DefaulterSummaryDto> defaulters = new ArrayList<>();
        for (Map.Entry<Long, List<MaintenanceBill>> entry : flatBillsMap.entrySet()) {
            List<MaintenanceBill> flatBills = entry.getValue();
            if (flatBills.isEmpty()) continue;

            Flat flat = flatBills.get(0).getFlat();
            Tower tower = flat.getTower();

            BigDecimal outstanding = BigDecimal.ZERO;
            LocalDate oldestDueDate = null;
            long maxDaysOverdue = 0;

            for (MaintenanceBill b : flatBills) {
                outstanding = outstanding.add(b.getOutstandingBalance());
                if (b.getDueDate() != null) {
                    if (oldestDueDate == null || b.getDueDate().isBefore(oldestDueDate)) {
                        oldestDueDate = b.getDueDate();
                    }
                    if (today.isAfter(b.getDueDate())) {
                        long d = ChronoUnit.DAYS.between(b.getDueDate(), today);
                        if (d > maxDaysOverdue) {
                            maxDaysOverdue = d;
                        }
                    }
                }
            }

            // Find last payment date if any
            LocalDate lastPaymentDate = null;
            List<Payment> payments = paymentRepository.findByFlatIdOrderByPaymentDateDesc(flat.getId());
            if (!payments.isEmpty() && payments.get(0).getPaymentDate() != null) {
                lastPaymentDate = payments.get(0).getPaymentDate().toLocalDate();
            }

            defaulters.add(new DefaulterSummaryDto(
                    flat.getId(),
                    tower != null ? tower.getId() : "N/A",
                    tower != null ? tower.getName() : "N/A",
                    flat.getFlatNumber(),
                    flat.getAreaSqFt(),
                    outstanding,
                    maxDaysOverdue,
                    oldestDueDate,
                    lastPaymentDate
            ));
        }

        // Sort by outstanding dues descending
        defaulters.sort((a, b) -> b.outstandingDues().compareTo(a.outstandingDues()));
        return defaulters;
    }

    // =========================================================================
    // 7. Complaint Analytics (Response times, SLA breach, categories)
    // =========================================================================
    public ComplaintAnalyticsDto getComplaintAnalytics(LocalDate fromDate, LocalDate toDate, String towerId) {
        List<Complaint> complaints = filterComplaints(fromDate, toDate, towerId);
        List<ComplaintActivity> allActivities = complaintActivityRepository.findAll();

        Map<Long, List<ComplaintActivity>> activityMap = allActivities.stream()
                .filter(a -> a.getComplaint() != null)
                .collect(Collectors.groupingBy(a -> a.getComplaint().getId()));

        long openCount = 0;
        long assignedCount = 0;
        long inProgressCount = 0;
        long onHoldCount = 0;
        long resolvedCount = 0;
        long closedCount = 0;
        long criticalEscalationsCount = 0;
        long slaBreachCount = 0;

        Map<String, Long> categoryBreakdown = new LinkedHashMap<>();
        for (ComplaintCategory cat : ComplaintCategory.values()) {
            categoryBreakdown.put(cat.name(), 0L);
        }

        Map<String, Long> priorityBreakdown = new LinkedHashMap<>();
        for (ComplaintPriority p : ComplaintPriority.values()) {
            priorityBreakdown.put(p.name(), 0L);
        }

        double totalFirstResponseHours = 0.0;
        long firstResponseSamples = 0;

        double totalResolutionHours = 0.0;
        long resolutionSamples = 0;

        LocalDateTime now = LocalDateTime.now();

        for (Complaint c : complaints) {
            // Status counts
            if (c.getStatus() == ComplaintStatus.SUBMITTED) openCount++;
            else if (c.getStatus() == ComplaintStatus.ASSIGNED) assignedCount++;
            else if (c.getStatus() == ComplaintStatus.IN_PROGRESS) inProgressCount++;
            else if (c.getStatus() == ComplaintStatus.ON_HOLD) onHoldCount++;
            else if (c.getStatus() == ComplaintStatus.RESOLVED) resolvedCount++;
            else if (c.getStatus() == ComplaintStatus.CLOSED) closedCount++;

            if (c.isCriticalSafetyEscalation()) {
                criticalEscalationsCount++;
            }

            if (c.getCategory() != null) {
                categoryBreakdown.put(c.getCategory().name(), categoryBreakdown.getOrDefault(c.getCategory().name(), 0L) + 1);
            }

            if (c.getPriority() != null) {
                priorityBreakdown.put(c.getPriority().name(), priorityBreakdown.getOrDefault(c.getPriority().name(), 0L) + 1);
            }

            // First response time calculation
            LocalDateTime firstResponseTime = c.getAcknowledgedAt();
            if (firstResponseTime == null) {
                List<ComplaintActivity> activities = activityMap.getOrDefault(c.getId(), Collections.emptyList());
                for (ComplaintActivity act : activities) {
                    if (act.getTimestamp() != null &&
                            ("HELP_DESK".equalsIgnoreCase(act.getActorRole()) ||
                                    !"SUBMITTED".equalsIgnoreCase(act.getActivityType()))) {
                        firstResponseTime = act.getTimestamp();
                        break;
                    }
                }
            }

            if (firstResponseTime != null && c.getCreatedAt() != null) {
                long minutes = Math.max(0, Duration.between(c.getCreatedAt(), firstResponseTime).toMinutes());
                totalFirstResponseHours += (minutes / 60.0);
                firstResponseSamples++;
            }

            // Resolution time calculation
            if (c.getResolvedAt() != null && c.getCreatedAt() != null) {
                long minutes = Math.max(0, Duration.between(c.getCreatedAt(), c.getResolvedAt()).toMinutes());
                totalResolutionHours += (minutes / 60.0);
                resolutionSamples++;
            }

            // SLA breach check
            boolean breached = false;
            if (c.getResolutionDeadline() != null) {
                if (c.getResolvedAt() != null) {
                    if (c.getResolvedAt().isAfter(c.getResolutionDeadline())) {
                        breached = true;
                    }
                } else if (c.getStatus() != ComplaintStatus.RESOLVED && c.getStatus() != ComplaintStatus.CLOSED) {
                    if (now.isAfter(c.getResolutionDeadline())) {
                        breached = true;
                    }
                }
            }

            if (c.getResponseDeadline() != null && firstResponseTime != null && firstResponseTime.isAfter(c.getResponseDeadline())) {
                breached = true;
            }

            if (breached) {
                slaBreachCount++;
            }
        }

        double avgFirstResponseHours = firstResponseSamples > 0
                ? BigDecimal.valueOf(totalFirstResponseHours / firstResponseSamples).setScale(1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        double avgResolutionHours = resolutionSamples > 0
                ? BigDecimal.valueOf(totalResolutionHours / resolutionSamples).setScale(1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        return new ComplaintAnalyticsDto(
                complaints.size(),
                openCount,
                assignedCount,
                inProgressCount,
                onHoldCount,
                resolvedCount,
                closedCount,
                avgFirstResponseHours,
                avgResolutionHours,
                slaBreachCount,
                criticalEscalationsCount,
                categoryBreakdown,
                priorityBreakdown
        );
    }

    // =========================================================================
    // 8. Operational Analytics (Visitors, Amenities, Notices)
    // =========================================================================
    public OperationalAnalyticsDto getOperationalAnalytics(LocalDate fromDate, LocalDate toDate, String towerId) {
        List<Visitor> visitors = filterVisitors(fromDate, toDate, towerId);
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        long todayCheckIns = 0;
        long checkedInCurrently = 0;
        long checkedOut = 0;
        long preApprovedCount = 0;
        long walkInCount = 0;

        for (Visitor v : visitors) {
            if (v.getCheckInTime() != null && !v.getCheckInTime().isBefore(startOfToday)) {
                todayCheckIns++;
            }
            if (v.getStatus() == VisitorStatus.CHECKED_IN) {
                checkedInCurrently++;
            } else if (v.getStatus() == VisitorStatus.CHECKED_OUT) {
                checkedOut++;
            }

            if (v.getResident() == null) {
                walkInCount++;
            } else {
                preApprovedCount++;
            }
        }

        // Amenities
        List<AmenityBooking> bookings = filterBookings(fromDate, toDate, towerId);
        long confirmedBookings = 0;
        long cancelledBookings = 0;

        Map<Long, Long> amenityBookingCountMap = new HashMap<>();
        Map<Long, Long> amenityAttendeesMap = new HashMap<>();

        for (AmenityBooking b : bookings) {
            if (b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.COMPLETED) {
                confirmedBookings++;
            } else if (b.getStatus() == BookingStatus.CANCELLED) {
                cancelledBookings++;
            }

            if (b.getAmenity() != null) {
                Long aid = b.getAmenity().getId();
                amenityBookingCountMap.put(aid, amenityBookingCountMap.getOrDefault(aid, 0L) + 1);
                amenityAttendeesMap.put(aid, amenityAttendeesMap.getOrDefault(aid, 0L) + b.getAttendeeCount());
            }
        }

        double cancellationRate = 0.0;
        if (!bookings.isEmpty()) {
            cancellationRate = BigDecimal.valueOf(cancelledBookings * 100.0 / bookings.size())
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        List<Amenity> allAmenities = amenityRepository.findAll();
        List<AmenityUsageStatDto> topAmenities = allAmenities.stream()
                .map(a -> new AmenityUsageStatDto(
                        a.getId(),
                        a.getName(),
                        a.getCategory() != null ? a.getCategory().name() : "GENERAL",
                        amenityBookingCountMap.getOrDefault(a.getId(), 0L),
                        amenityAttendeesMap.getOrDefault(a.getId(), 0L)
                ))
                .sorted((a, b) -> Long.compare(b.bookingCount(), a.bookingCount()))
                .limit(5)
                .toList();

        // Notices
        List<Notice> notices = filterNotices(fromDate, toDate, towerId);
        LocalDateTime now = LocalDateTime.now();

        long activeNotices = notices.stream()
                .filter(n -> n.getStatus() == NoticeStatus.PUBLISHED && (n.getExpiryDate() == null || n.getExpiryDate().isAfter(now)))
                .count();

        long totalNoticeReads = noticeReadRepository.count();
        long totalFlats = flatRepository.count();
        long publishedCount = notices.stream().filter(n -> n.getStatus() == NoticeStatus.PUBLISHED).count();

        double noticeEngagementRate = 0.0;
        if (publishedCount > 0 && totalFlats > 0) {
            long maxPotentialReads = publishedCount * totalFlats;
            noticeEngagementRate = Math.min(100.0, BigDecimal.valueOf((totalNoticeReads * 100.0) / maxPotentialReads)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue());
        }

        // Tower operational comparison
        List<Tower> towers = towerRepository.findAllByOrderByIdAsc();
        List<Complaint> allComplaints = complaintRepository.findAll();
        List<Visitor> allVisitors = visitorRepository.findAll();
        List<MaintenanceBill> allBills = billRepository.findAll();

        Map<String, Long> towerVisitorMap = allVisitors.stream()
                .filter(v -> v.getFlat() != null && v.getFlat().getTower() != null)
                .collect(Collectors.groupingBy(v -> v.getFlat().getTower().getId(), Collectors.counting()));

        Map<String, Long> towerComplaintMap = allComplaints.stream()
                .filter(c -> c.getFlat() != null && c.getFlat().getTower() != null)
                .collect(Collectors.groupingBy(c -> c.getFlat().getTower().getId(), Collectors.counting()));

        Map<String, BigDecimal> towerDuesMap = new HashMap<>();
        for (MaintenanceBill b : allBills) {
            if (b.getFlat() != null && b.getFlat().getTower() != null && b.getOutstandingBalance() != null) {
                String tid = b.getFlat().getTower().getId();
                towerDuesMap.put(tid, towerDuesMap.getOrDefault(tid, BigDecimal.ZERO).add(b.getOutstandingBalance()));
            }
        }

        List<TowerOperationalSummaryDto> towerComparison = towers.stream()
                .map(t -> new TowerOperationalSummaryDto(
                        t.getId(),
                        t.getName(),
                        towerVisitorMap.getOrDefault(t.getId(), 0L),
                        towerComplaintMap.getOrDefault(t.getId(), 0L),
                        towerDuesMap.getOrDefault(t.getId(), BigDecimal.ZERO)
                ))
                .toList();

        return new OperationalAnalyticsDto(
                visitors.size(),
                todayCheckIns,
                checkedInCurrently,
                checkedOut,
                preApprovedCount,
                walkInCount,
                bookings.size(),
                confirmedBookings,
                cancelledBookings,
                cancellationRate,
                topAmenities,
                notices.size(),
                activeNotices,
                totalNoticeReads,
                noticeEngagementRate,
                towerComparison
        );
    }

    // =========================================================================
    // 9. CSV Reports Generation
    // =========================================================================
    public byte[] generateFinancialCsv(LocalDate fromDate, LocalDate toDate, String towerId) {
        List<MaintenanceBill> bills = filterBills(fromDate, toDate, towerId);
        StringBuilder sb = new StringBuilder();
        sb.append("Billing Month,Tower ID,Tower Name,Flat Number,Total Due (INR),Paid Amount (INR),Outstanding Balance (INR),Status,Issue Date,Due Date\n");

        for (MaintenanceBill b : bills) {
            sb.append(escapeCsv(b.getBillingMonth())).append(",");
            sb.append(escapeCsv(b.getFlat() != null && b.getFlat().getTower() != null ? b.getFlat().getTower().getId() : "N/A")).append(",");
            sb.append(escapeCsv(b.getFlat() != null && b.getFlat().getTower() != null ? b.getFlat().getTower().getName() : "N/A")).append(",");
            sb.append(escapeCsv(b.getFlat() != null ? b.getFlat().getFlatNumber() : "N/A")).append(",");
            sb.append(b.getTotalDue() != null ? b.getTotalDue().toPlainString() : "0.00").append(",");
            sb.append(b.getPaidAmount() != null ? b.getPaidAmount().toPlainString() : "0.00").append(",");
            sb.append(b.getOutstandingBalance() != null ? b.getOutstandingBalance().toPlainString() : "0.00").append(",");
            sb.append(escapeCsv(b.getStatus() != null ? b.getStatus().name() : "N/A")).append(",");
            sb.append(b.getIssueDate() != null ? b.getIssueDate().toString() : "").append(",");
            sb.append(b.getDueDate() != null ? b.getDueDate().toString() : "").append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] generateDefaultersCsv(String towerId) {
        List<DefaulterSummaryDto> defaulters = getDefaulterSummaries(towerId);
        StringBuilder sb = new StringBuilder();
        sb.append("Tower ID,Tower Name,Flat Number,Carpet Area (sq ft),Outstanding Dues (INR),Oldest Due Date,Days Overdue,Last Payment Date\n");

        for (DefaulterSummaryDto d : defaulters) {
            sb.append(escapeCsv(d.towerId())).append(",");
            sb.append(escapeCsv(d.towerName())).append(",");
            sb.append(escapeCsv(d.flatNumber())).append(",");
            sb.append(d.carpetArea()).append(",");
            sb.append(d.outstandingDues() != null ? d.outstandingDues().toPlainString() : "0.00").append(",");
            sb.append(d.oldestDueDate() != null ? d.oldestDueDate().toString() : "").append(",");
            sb.append(d.daysOverdue()).append(",");
            sb.append(d.lastPaymentDate() != null ? d.lastPaymentDate().toString() : "None").append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] generateComplaintsCsv(LocalDate fromDate, LocalDate toDate, String towerId) {
        List<Complaint> complaints = filterComplaints(fromDate, toDate, towerId);
        StringBuilder sb = new StringBuilder();
        sb.append("Complaint Number,Title,Tower,Flat Number,Category,Priority,Status,Critical Safety Escalation,Created At,Acknowledged At,Resolved At,Resolution Notes\n");

        for (Complaint c : complaints) {
            sb.append(escapeCsv(c.getComplaintNumber())).append(",");
            sb.append(escapeCsv(c.getTitle())).append(",");
            sb.append(escapeCsv(c.getFlat() != null && c.getFlat().getTower() != null ? c.getFlat().getTower().getId() : "N/A")).append(",");
            sb.append(escapeCsv(c.getFlat() != null ? c.getFlat().getFlatNumber() : "N/A")).append(",");
            sb.append(escapeCsv(c.getCategory() != null ? c.getCategory().name() : "N/A")).append(",");
            sb.append(escapeCsv(c.getPriority() != null ? c.getPriority().name() : "N/A")).append(",");
            sb.append(escapeCsv(c.getStatus() != null ? c.getStatus().name() : "N/A")).append(",");
            sb.append(c.isCriticalSafetyEscalation() ? "YES" : "NO").append(",");
            sb.append(c.getCreatedAt() != null ? c.getCreatedAt().toString() : "").append(",");
            sb.append(c.getAcknowledgedAt() != null ? c.getAcknowledgedAt().toString() : "").append(",");
            sb.append(c.getResolvedAt() != null ? c.getResolvedAt().toString() : "").append(",");
            sb.append(escapeCsv(c.getResolutionNotes() != null ? c.getResolutionNotes() : "")).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] generateOperationalCsv(LocalDate fromDate, LocalDate toDate, String towerId) {
        List<TowerOperationalSummaryDto> towerSummaries = getOperationalAnalytics(fromDate, toDate, towerId).towerOperationalComparison();
        StringBuilder sb = new StringBuilder();
        sb.append("Tower ID,Tower Name,Total Visitors,Total Complaints,Outstanding Dues (INR)\n");

        for (TowerOperationalSummaryDto t : towerSummaries) {
            sb.append(escapeCsv(t.towerId())).append(",");
            sb.append(escapeCsv(t.towerName())).append(",");
            sb.append(t.visitorCount()).append(",");
            sb.append(t.complaintCount()).append(",");
            sb.append(t.outstandingDues() != null ? t.outstandingDues().toPlainString() : "0.00").append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================
    private List<MaintenanceBill> filterBills(LocalDate fromDate, LocalDate toDate, String towerId) {
        List<MaintenanceBill> bills = billRepository.findAll();
        return bills.stream()
                .filter(b -> {
                    if (towerId != null && !towerId.equalsIgnoreCase("ALL") && !towerId.isBlank()) {
                        if (b.getFlat() == null || b.getFlat().getTower() == null ||
                                !b.getFlat().getTower().getId().equalsIgnoreCase(towerId)) {
                            return false;
                        }
                    }
                    if (fromDate != null && b.getIssueDate() != null && b.getIssueDate().isBefore(fromDate)) {
                        return false;
                    }
                    if (toDate != null && b.getIssueDate() != null && b.getIssueDate().isAfter(toDate)) {
                        return false;
                    }
                    return true;
                })
                .toList();
    }

    private List<Complaint> filterComplaints(LocalDate fromDate, LocalDate toDate, String towerId) {
        List<Complaint> complaints = complaintRepository.findAll();
        return complaints.stream()
                .filter(c -> {
                    if (towerId != null && !towerId.equalsIgnoreCase("ALL") && !towerId.isBlank()) {
                        if (c.getFlat() == null || c.getFlat().getTower() == null ||
                                !c.getFlat().getTower().getId().equalsIgnoreCase(towerId)) {
                            return false;
                        }
                    }
                    if (fromDate != null && c.getCreatedAt() != null && c.getCreatedAt().toLocalDate().isBefore(fromDate)) {
                        return false;
                    }
                    if (toDate != null && c.getCreatedAt() != null && c.getCreatedAt().toLocalDate().isAfter(toDate)) {
                        return false;
                    }
                    return true;
                })
                .toList();
    }

    private List<Visitor> filterVisitors(LocalDate fromDate, LocalDate toDate, String towerId) {
        List<Visitor> visitors = visitorRepository.findAll();
        return visitors.stream()
                .filter(v -> {
                    if (towerId != null && !towerId.equalsIgnoreCase("ALL") && !towerId.isBlank()) {
                        if (v.getFlat() == null || v.getFlat().getTower() == null ||
                                !v.getFlat().getTower().getId().equalsIgnoreCase(towerId)) {
                            return false;
                        }
                    }
                    if (fromDate != null && v.getCreatedAt() != null && v.getCreatedAt().toLocalDate().isBefore(fromDate)) {
                        return false;
                    }
                    if (toDate != null && v.getCreatedAt() != null && v.getCreatedAt().toLocalDate().isAfter(toDate)) {
                        return false;
                    }
                    return true;
                })
                .toList();
    }

    private List<AmenityBooking> filterBookings(LocalDate fromDate, LocalDate toDate, String towerId) {
        List<AmenityBooking> bookings = amenityBookingRepository.findAll();
        return bookings.stream()
                .filter(b -> {
                    if (towerId != null && !towerId.equalsIgnoreCase("ALL") && !towerId.isBlank()) {
                        if (b.getFlat() == null || b.getFlat().getTower() == null ||
                                !b.getFlat().getTower().getId().equalsIgnoreCase(towerId)) {
                            return false;
                        }
                    }
                    if (fromDate != null && b.getBookingDate() != null && b.getBookingDate().isBefore(fromDate)) {
                        return false;
                    }
                    if (toDate != null && b.getBookingDate() != null && b.getBookingDate().isAfter(toDate)) {
                        return false;
                    }
                    return true;
                })
                .toList();
    }

    private List<Notice> filterNotices(LocalDate fromDate, LocalDate toDate, String towerId) {
        List<Notice> notices = noticeRepository.findAll();
        return notices.stream()
                .filter(n -> {
                    if (towerId != null && !towerId.equalsIgnoreCase("ALL") && !towerId.isBlank()) {
                        if (n.getTargetTower() != null && !n.getTargetTower().getId().equalsIgnoreCase(towerId)) {
                            return false;
                        }
                    }
                    if (fromDate != null && n.getPublishDate() != null && n.getPublishDate().toLocalDate().isBefore(fromDate)) {
                        return false;
                    }
                    if (toDate != null && n.getPublishDate() != null && n.getPublishDate().toLocalDate().isAfter(toDate)) {
                        return false;
                    }
                    return true;
                })
                .toList();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
