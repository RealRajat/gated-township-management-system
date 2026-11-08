package com.township.config;

import com.township.entity.*;
import com.township.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(name = "app.seeder.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final TowerRepository towerRepository;
    private final FlatRepository flatRepository;
    private final UserRepository userRepository;
    private final FlatMemberRepository flatMemberRepository;
    private final BillingConfigurationRepository configRepository;
    private final MaintenanceBillRepository billRepository;
    private final FlatLedgerEntryRepository ledgerRepository;
    private final PaymentRepository paymentRepository;
    private final ComplaintRepository complaintRepository;
    private final ComplaintActivityRepository complaintActivityRepository;
    private final VisitorRepository visitorRepository;
    private final VisitorActivityRepository visitorActivityRepository;
    private final AmenityRepository amenityRepository;
    private final AmenityBookingRepository amenityBookingRepository;
    private final NoticeRepository noticeRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            TowerRepository towerRepository,
            FlatRepository flatRepository,
            UserRepository userRepository,
            FlatMemberRepository flatMemberRepository,
            BillingConfigurationRepository configRepository,
            MaintenanceBillRepository billRepository,
            FlatLedgerEntryRepository ledgerRepository,
            PaymentRepository paymentRepository,
            ComplaintRepository complaintRepository,
            ComplaintActivityRepository complaintActivityRepository,
            VisitorRepository visitorRepository,
            VisitorActivityRepository visitorActivityRepository,
            AmenityRepository amenityRepository,
            AmenityBookingRepository amenityBookingRepository,
            NoticeRepository noticeRepository,
            PasswordEncoder passwordEncoder) {
        this.towerRepository = towerRepository;
        this.flatRepository = flatRepository;
        this.userRepository = userRepository;
        this.flatMemberRepository = flatMemberRepository;
        this.configRepository = configRepository;
        this.billRepository = billRepository;
        this.ledgerRepository = ledgerRepository;
        this.paymentRepository = paymentRepository;
        this.complaintRepository = complaintRepository;
        this.complaintActivityRepository = complaintActivityRepository;
        this.visitorRepository = visitorRepository;
        this.visitorActivityRepository = visitorActivityRepository;
        this.amenityRepository = amenityRepository;
        this.amenityBookingRepository = amenityBookingRepository;
        this.noticeRepository = noticeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Starting idempotent database seeding for Pune Grand Vista Township...");
        seedTowers();
        seedSampleFlatsAndResidents();
        seedHelpDeskStaff();
        seedGateGuardStaff();
        seedCommitteeStaff();
        seedBillingConfiguration();
        seedSampleBillsAndLedgers();
        seedSampleComplaints();
        seedSampleVisitors();
        seedAmenities();
        seedSampleBookings();
        seedNotices();
        log.info("Database seeding completed successfully.");
    }

    private void seedTowers() {
        String[][] towersData = {
                {"T01", "Tower A (Orchid)", "15", "86"},
                {"T02", "Tower B (Marigold)", "15", "86"},
                {"T03", "Tower C (Hibiscus)", "15", "86"},
                {"T04", "Tower D (Jasmine)", "15", "86"},
                {"T05", "Tower E (Lotus)", "15", "86"},
                {"T06", "Tower F (Tulip)", "15", "86"},
                {"T07", "Tower G (Iris)", "15", "86"},
                {"T08", "Tower H (Aster)", "15", "86"},
                {"T09", "Tower I (Lavender)", "15", "86"},
                {"T10", "Tower J (Dahlia)", "15", "86"},
                {"T11", "Tower K (Carnation)", "15", "86"},
                {"T12", "Tower L (Magnolia)", "15", "86"},
                {"T13", "Tower M (Gardenia)", "15", "86"},
                {"T14", "Tower N (Begonia)", "15", "86"},
                {"T15", "Tower O (Camellia)", "15", "86"},
                {"T16", "Tower P (Primrose)", "15", "86"},
                {"T17", "Tower Q (Bluebell)", "15", "86"},
                {"T18", "Tower R (Snapdragon)", "15", "86"},
                {"T19", "Tower S (Zinnia)", "15", "86"},
                {"T20", "Tower T (Petunia)", "15", "86"},
                {"T21", "Tower U (Foxglove)", "15", "86"},
                {"T22", "Tower V (Freesia)", "15", "86"},
                {"T23", "Tower W (Anemone)", "15", "86"},
                {"T24", "Tower X (Violet)", "15", "86"},
                {"T25", "Tower Y (Pansy)", "15", "86"},
                {"T26", "Tower Z (Lupin)", "15", "86"},
                {"T27", "Tower AA (Daffodil)", "15", "82"},
                {"T28", "Tower AB (Sunflower)", "15", "82"}
        };

        for (String[] t : towersData) {
            String id = t[0];
            String name = t[1];
            int floors = Integer.parseInt(t[2]);
            int units = Integer.parseInt(t[3]);

            if (!towerRepository.existsById(id)) {
                towerRepository.save(new Tower(id, name, floors, units, "Residential"));
            }
        }
    }

    private void seedSampleFlatsAndResidents() {
        // Resident 1: T07-1203, Neeraj Kulkarni, 1150 sq ft
        seedResidentWithFlat(
                "T07",
                "1203",
                12,
                1150.0,
                "Neeraj Kulkarni",
                "neeraj.kulkarni@grandvistapune.in",
                "+919822012031",
                "Resident@123"
        );

        // Resident 2: T12-0408, Swati Joshi, 980 sq ft
        seedResidentWithFlat(
                "T12",
                "0408",
                4,
                980.0,
                "Swati Joshi",
                "swati.joshi@grandvistapune.in",
                "+919822004082",
                "Resident@123"
        );

        // Resident 3: T21-1601, Aamir Shaikh, 1640 sq ft
        seedResidentWithFlat(
                "T21",
                "1601",
                16,
                1640.0,
                "Aamir Shaikh",
                "aamir.shaikh@grandvistapune.in",
                "+919822016013",
                "Resident@123"
        );
    }

    private void seedResidentWithFlat(
            String towerId,
            String flatNumber,
            int floorNumber,
            double areaSqFt,
            String residentName,
            String email,
            String phone,
            String password) {

        Tower tower = towerRepository.findById(towerId).orElse(null);
        if (tower == null) {
            log.warn("Tower {} not found, skipping resident seeding for {}", towerId, residentName);
            return;
        }

        Flat flat = flatRepository.findByTowerIdAndFlatNumber(towerId, flatNumber)
                .orElseGet(() -> flatRepository.save(new Flat(tower, flatNumber, floorNumber, areaSqFt, "OWNER_OCCUPIED")));

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            String hashed = passwordEncoder.encode(password);
            return userRepository.save(new User(residentName, email, phone, hashed, Role.RESIDENT));
        });

        if (!flatMemberRepository.existsByFlatIdAndUserId(flat.getId(), user.getId())) {
            flatMemberRepository.save(new FlatMember(flat, user, "PRIMARY_OWNER", true));
        }
    }

    private void seedHelpDeskStaff() {
        String helpDeskEmail = "helpdesk@grandvistapune.in";
        if (!userRepository.existsByEmail(helpDeskEmail)) {
            String hashed = passwordEncoder.encode("HelpDesk@123");
            userRepository.save(new User(
                    "Col. Rajiv Sharma (Retd.)",
                    helpDeskEmail,
                    "+919822000100",
                    hashed,
                    Role.HELP_DESK
            ));
        }
    }

    private void seedBillingConfiguration() {
        if (configRepository.count() == 0) {
            BillingConfiguration config = new BillingConfiguration(
                    new BigDecimal("6.50"),
                    15,
                    "PERCENTAGE",
                    new BigDecimal("10.00"),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    true
            );
            configRepository.save(config);
            log.info("Initialized default billing configuration with INR 6.50/sq.ft base rate.");
        }
    }

    private void seedSampleBillsAndLedgers() {
        LocalDate today = LocalDate.now();

        // 1. T07-1203: 1150 sq ft, bill INR 7475, paid INR 7475 (Status: PAID)
        Flat flat1 = flatRepository.findByTowerIdAndFlatNumber("T07", "1203").orElse(null);
        if (flat1 != null && !billRepository.existsByFlatIdAndBillingMonth(flat1.getId(), "2026-10")) {
            BigDecimal area = new BigDecimal("1150.00");
            BigDecimal rate = new BigDecimal("6.50");
            BigDecimal amount = area.multiply(rate).setScale(2, RoundingMode.HALF_UP); // 7475.00
            BigDecimal paid = new BigDecimal("7475.00");
            BigDecimal outstanding = BigDecimal.ZERO;

            LocalDate issueDate = LocalDate.of(2026, 10, 1);
            LocalDate dueDate = LocalDate.of(2026, 10, 16);

            MaintenanceBill bill = new MaintenanceBill(
                    flat1,
                    "2026-10",
                    area,
                    rate,
                    amount,
                    BigDecimal.ZERO,
                    amount,
                    paid,
                    outstanding,
                    BillStatus.PAID,
                    issueDate,
                    dueDate,
                    "October 2026 Maintenance Bill (Settled in full)"
            );
            bill = billRepository.save(bill);

            // Ledger entry 1: Bill Generation (Debit 7475.00)
            ledgerRepository.save(new FlatLedgerEntry(
                    flat1,
                    bill,
                    issueDate.atTime(9, 0),
                    LedgerEntryType.BILL_GENERATION,
                    "Monthly Maintenance Bill - 2026-10 (1150 sq.ft @ ₹6.50)",
                    amount,
                    BigDecimal.ZERO,
                    amount,
                    "GEN-202610-T07-1203"
            ));

            // Ledger entry 2: Payment Received (Credit 7475.00)
            ledgerRepository.save(new FlatLedgerEntry(
                    flat1,
                    bill,
                    issueDate.plusDays(4).atTime(14, 30),
                    LedgerEntryType.PAYMENT,
                    "Maintenance Bill Payment - UPI Demo Settlement",
                    BigDecimal.ZERO,
                    paid,
                    BigDecimal.ZERO,
                    "DEMO-UPI-7475"
            ));

            User resident = userRepository.findByEmail("neeraj.kulkarni@grandvistapune.in").orElse(null);
            paymentRepository.save(new Payment(
                    bill,
                    flat1,
                    resident,
                    paid,
                    issueDate.plusDays(4).atTime(14, 30),
                    "UPI",
                    "DEMO-UPI-7475",
                    "COMPLETED",
                    "Settled via UPI (Phase 4/5 Demo Record)"
            ));
        }

        // 2. T12-0408: 980 sq ft, bill INR 6370, paid INR 0, 47 days overdue (Status: OVERDUE)
        Flat flat2 = flatRepository.findByTowerIdAndFlatNumber("T12", "0408").orElse(null);
        if (flat2 != null && !billRepository.existsByFlatIdAndBillingMonth(flat2.getId(), "2026-09")) {
            BigDecimal area = new BigDecimal("980.00");
            BigDecimal rate = new BigDecimal("6.50");
            BigDecimal amount = area.multiply(rate).setScale(2, RoundingMode.HALF_UP); // 6370.00
            BigDecimal paid = BigDecimal.ZERO;
            BigDecimal outstanding = amount;

            LocalDate dueDate = today.minusDays(47);
            LocalDate issueDate = dueDate.minusDays(15);

            MaintenanceBill bill = new MaintenanceBill(
                    flat2,
                    "2026-09",
                    area,
                    rate,
                    amount,
                    BigDecimal.ZERO,
                    amount,
                    paid,
                    outstanding,
                    BillStatus.OVERDUE,
                    issueDate,
                    dueDate,
                    "September 2026 Maintenance Bill (47 days overdue)"
            );
            bill = billRepository.save(bill);

            ledgerRepository.save(new FlatLedgerEntry(
                    flat2,
                    bill,
                    issueDate.atTime(9, 0),
                    LedgerEntryType.BILL_GENERATION,
                    "Monthly Maintenance Bill - 2026-09 (980 sq.ft @ ₹6.50)",
                    amount,
                    BigDecimal.ZERO,
                    amount,
                    "GEN-202609-T12-0408"
            ));
        }

        // 3. T21-1601: 1640 sq ft, bill INR 10660, paid INR 5000, 18 days overdue (Status: PARTIALLY_PAID)
        Flat flat3 = flatRepository.findByTowerIdAndFlatNumber("T21", "1601").orElse(null);
        if (flat3 != null && !billRepository.existsByFlatIdAndBillingMonth(flat3.getId(), "2026-10")) {
            BigDecimal area = new BigDecimal("1640.00");
            BigDecimal rate = new BigDecimal("6.50");
            BigDecimal amount = area.multiply(rate).setScale(2, RoundingMode.HALF_UP); // 10660.00
            BigDecimal paid = new BigDecimal("5000.00");
            BigDecimal outstanding = new BigDecimal("5660.00");

            LocalDate dueDate = today.minusDays(18);
            LocalDate issueDate = dueDate.minusDays(15);

            MaintenanceBill bill = new MaintenanceBill(
                    flat3,
                    "2026-10",
                    area,
                    rate,
                    amount,
                    BigDecimal.ZERO,
                    amount,
                    paid,
                    outstanding,
                    BillStatus.PARTIALLY_PAID,
                    issueDate,
                    dueDate,
                    "October 2026 Maintenance Bill (Partially paid, 18 days overdue balance)"
            );
            bill = billRepository.save(bill);

            // Ledger entry 1: Bill Generation (Debit 10660.00)
            ledgerRepository.save(new FlatLedgerEntry(
                    flat3,
                    bill,
                    issueDate.atTime(9, 0),
                    LedgerEntryType.BILL_GENERATION,
                    "Monthly Maintenance Bill - 2026-10 (1640 sq.ft @ ₹6.50)",
                    amount,
                    BigDecimal.ZERO,
                    amount,
                    "GEN-202610-T21-1601"
            ));

            // Ledger entry 2: Partial Payment Received (Credit 5000.00)
            ledgerRepository.save(new FlatLedgerEntry(
                    flat3,
                    bill,
                    issueDate.plusDays(5).atTime(11, 15),
                    LedgerEntryType.PAYMENT,
                    "Partial Maintenance Bill Payment - NEFT Demo Settlement",
                    BigDecimal.ZERO,
                    paid,
                    outstanding,
                    "DEMO-NEFT-5000"
            ));

            User resident = userRepository.findByEmail("aamir.shaikh@grandvistapune.in").orElse(null);
            paymentRepository.save(new Payment(
                    bill,
                    flat3,
                    resident,
                    paid,
                    issueDate.plusDays(5).atTime(11, 15),
                    "NEFT",
                    "DEMO-NEFT-5000",
                    "COMPLETED",
                    "Partial payment via NEFT (Phase 4/5 Demo Record)"
            ));
        }
    }

    private void seedSampleComplaints() {
        if (complaintRepository.count() > 0) {
            return;
        }

        Flat flat1 = flatRepository.findByTowerIdAndFlatNumber("T07", "1203").orElse(null);
        Flat flat2 = flatRepository.findByTowerIdAndFlatNumber("T12", "0408").orElse(null);
        Flat flat3 = flatRepository.findByTowerIdAndFlatNumber("T21", "1601").orElse(null);
        User neeraj = userRepository.findByEmail("neeraj.kulkarni@grandvistapune.in").orElse(null);
        User swati = userRepository.findByEmail("swati.joshi@grandvistapune.in").orElse(null);
        User aamir = userRepository.findByEmail("aamir.shaikh@grandvistapune.in").orElse(null);
        User helpDesk = userRepository.findByEmail("helpdesk@grandvistapune.in").orElse(null);

        if (flat1 == null || neeraj == null || flat2 == null || swati == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. Critical Safety Escalation: Lift Stuck
        Complaint c1 = new Complaint(
                "CMP-202611-0001",
                flat1,
                neeraj,
                "Passenger Lift Stuck between 8th and 9th floor",
                "Passenger lift stuck with people trapped inside including an elderly resident. Alarm ringing continuously.",
                ComplaintCategory.LIFT,
                ComplaintPriority.CRITICAL,
                true,
                "lift stuck",
                "LIFT",
                "CRITICAL",
                100,
                now.plusHours(1),
                now.plusHours(4)
        );
        c1.setStatus(ComplaintStatus.ASSIGNED);
        c1.setAssignedTeam("Otis Lift Rapid AMC Team");
        c1.setAssignedStaff(helpDesk);
        c1.setAcknowledgedAt(now.minusMinutes(25));
        c1 = complaintRepository.save(c1);

        complaintActivityRepository.save(new ComplaintActivity(
                c1,
                neeraj,
                neeraj.getFullName(),
                "RESIDENT",
                null,
                ComplaintStatus.SUBMITTED,
                "SAFETY_ESCALATED",
                "Complaint filed and CRITICALLY ESCALATED: Safety trigger keyword 'lift stuck'.",
                "Auto-triaged by SAFETY_RULE_ENGINE",
                false
        ));

        complaintActivityRepository.save(new ComplaintActivity(
                c1,
                helpDesk,
                helpDesk.getFullName(),
                "HELP_DESK",
                ComplaintStatus.SUBMITTED,
                ComplaintStatus.ASSIGNED,
                "ASSIGNED",
                "Assigned to Otis Lift Rapid AMC Team. Technician dispatched.",
                "Technician Ramesh deployed with master key.",
                false
        ));

        // 2. High Urgency: Water Riser Leakage (Swati Joshi)
        Complaint c2 = new Complaint(
                "CMP-202611-0002",
                flat2,
                swati,
                "Main vertical water pipe leakage in utility duct",
                "Continuous water leakage from main riser pipe in utility duct causing water accumulation and low pressure.",
                ComplaintCategory.PLUMBING,
                ComplaintPriority.HIGH,
                false,
                null,
                "PLUMBING",
                "HIGH",
                95,
                now.plusHours(4),
                now.plusHours(12)
        );
        c2.setStatus(ComplaintStatus.IN_PROGRESS);
        c2.setAssignedTeam("Township Plumbing Cell");
        c2.setAcknowledgedAt(now.minusHours(2));
        c2 = complaintRepository.save(c2);

        complaintActivityRepository.save(new ComplaintActivity(
                c2,
                swati,
                swati.getFullName(),
                "RESIDENT",
                null,
                ComplaintStatus.SUBMITTED,
                "CREATED",
                "Complaint filed for utility duct pipe leakage.",
                "Auto-triaged by GEMINI_AI (Confidence: 95%)",
                false
        ));

        // 3. Resolved Complaint with 5-Star Rating (Aamir Shaikh)
        if (flat3 != null && aamir != null) {
            Complaint c3 = new Complaint(
                    "CMP-202611-0003",
                    flat3,
                    aamir,
                    "Balcony ceiling light flickering and tripping MCB",
                    "Balcony waterproof ceiling lamp was flickering and causing MCB trip whenever switched on.",
                    ComplaintCategory.ELECTRICAL,
                    ComplaintPriority.MEDIUM,
                    false,
                    null,
                    "ELECTRICAL",
                    "MEDIUM",
                    90,
                    now.minusDays(2).plusHours(12),
                    now.minusDays(2).plusHours(24)
            );
            c3.setStatus(ComplaintStatus.RESOLVED);
            c3.setAssignedTeam("Electrical Maintenance Unit 2");
            c3.setAcknowledgedAt(now.minusDays(2).plusHours(1));
            c3.setResolvedAt(now.minusDays(1));
            c3.setResolutionNotes("Replaced damaged weatherproof fitting and tightened earthing connection at distribution board.");
            c3.setResidentRating(5);
            c3.setResidentFeedback("Excellent rapid response and polite electrician. Issue resolved completely.");
            c3.setRatedAt(now.minusHours(18));
            c3 = complaintRepository.save(c3);

            complaintActivityRepository.save(new ComplaintActivity(
                    c3,
                    helpDesk,
                    helpDesk.getFullName(),
                    "HELP_DESK",
                    ComplaintStatus.IN_PROGRESS,
                    ComplaintStatus.RESOLVED,
                    "STATUS_CHANGED",
                    "Resolved: Replaced damaged weatherproof fitting and tightened earthing.",
                    "Internal inspection complete.",
                    false
            ));

            complaintActivityRepository.save(new ComplaintActivity(
                    c3,
                    aamir,
                    aamir.getFullName(),
                    "RESIDENT",
                    ComplaintStatus.RESOLVED,
                    ComplaintStatus.RESOLVED,
                    "RATED",
                    "Resident submitted 5-star rating. Feedback: Excellent rapid response and polite electrician.",
                    null,
                    false
            ));
        }

        log.info("Seeded initial sample complaints with safety escalation and activity timeline.");
    }

    private void seedGateGuardStaff() {
        String guardEmail = "guard@grandvistapune.in";
        if (!userRepository.existsByEmail(guardEmail)) {
            String hashed = passwordEncoder.encode("Guard@123");
            userRepository.save(new User(
                    "Santosh Shinde (Gate 1 Security)",
                    guardEmail,
                    "+919822000200",
                    hashed,
                    Role.GUARD
            ));
            log.info("Provisioned Gate Guard staff user: {}", guardEmail);
        }
    }

    private void seedCommitteeStaff() {
        String committeeEmail = "committee@grandvistapune.in";
        if (!userRepository.existsByEmail(committeeEmail)) {
            String hashed = passwordEncoder.encode("Committee@123");
            userRepository.save(new User(
                    "Dr. Vikram Sethi (MC President)",
                    committeeEmail,
                    "+919822000300",
                    hashed,
                    Role.COMMITTEE
            ));
            log.info("Provisioned Managing Committee user: {}", committeeEmail);
        }
    }

    private void seedSampleVisitors() {
        if (visitorRepository.count() > 0) {
            return;
        }

        Flat flat1 = flatRepository.findByTowerIdAndFlatNumber("T07", "1203").orElse(null);
        Flat flat2 = flatRepository.findByTowerIdAndFlatNumber("T12", "0408").orElse(null);
        Flat flat3 = flatRepository.findByTowerIdAndFlatNumber("T21", "1601").orElse(null);

        User neeraj = userRepository.findByEmail("neeraj.kulkarni@grandvistapune.in").orElse(null);
        User swati = userRepository.findByEmail("swati.joshi@grandvistapune.in").orElse(null);
        User aamir = userRepository.findByEmail("aamir.shaikh@grandvistapune.in").orElse(null);
        User guard = userRepository.findByEmail("guard@grandvistapune.in").orElse(null);

        LocalDateTime now = LocalDateTime.now();

        // 1. Pre-approved guest for Neeraj Kulkarni (T07-1203)
        if (flat1 != null && neeraj != null) {
            Visitor v1 = new Visitor(
                    "VP-892411",
                    "tok_rajesh_sharma_9824",
                    flat1,
                    neeraj,
                    "Rajesh Sharma",
                    "+919822019911",
                    VisitorType.GUEST,
                    "Weekend family visit & dinner",
                    "MH12-PQ-4455",
                    VisitorStatus.PRE_APPROVED,
                    now.minusHours(1),
                    now.plusDays(1),
                    true
            );
            v1 = visitorRepository.save(v1);
            visitorActivityRepository.save(new VisitorActivity(
                    v1,
                    "PRE_APPROVED",
                    neeraj,
                    "Resident Neeraj pre-approved family guest pass for Rajesh Sharma."
            ));
        }

        // 2. Currently checked-in delivery for Swati Joshi (T12-0408)
        if (flat2 != null && swati != null) {
            Visitor v2 = new Visitor(
                    "VP-551902",
                    "tok_amazon_delivery_5519",
                    flat2,
                    null,
                    "Rahul Verma (Amazon)",
                    "+919822044812",
                    VisitorType.DELIVERY,
                    "Amazon e-commerce package delivery",
                    "MH12-DL-8822",
                    VisitorStatus.CHECKED_IN,
                    now.minusMinutes(45),
                    now.plusHours(2),
                    true
            );
            v2.setCheckInTime(now.minusMinutes(30));
            v2.setEntryGuard(guard);
            v2.setEntryGate("Gate 1 (Main)");
            v2 = visitorRepository.save(v2);

            visitorActivityRepository.save(new VisitorActivity(
                    v2,
                    "REQUESTED",
                    guard,
                    "Walk-in delivery arrival at Gate 1."
            ));
            visitorActivityRepository.save(new VisitorActivity(
                    v2,
                    "APPROVED",
                    swati,
                    "Approved delivery entry by resident Swati Joshi."
            ));
            visitorActivityRepository.save(new VisitorActivity(
                    v2,
                    "CHECKED_IN",
                    guard,
                    "Checked in at Gate 1 by Guard Santosh Shinde."
            ));
        }

        // 3. Pending walk-in visitor for Neeraj Kulkarni (T07-1203)
        if (flat1 != null) {
            Visitor v3 = new Visitor(
                    "VP-338144",
                    "tok_urban_company_3381",
                    flat1,
                    null,
                    "Vikas Patil (Urban Company)",
                    "+919822088344",
                    VisitorType.SERVICE,
                    "Split AC servicing & filter replacement",
                    null,
                    VisitorStatus.PENDING_APPROVAL,
                    now,
                    now.plusHours(2),
                    true
            );
            v3.setEntryGate("Gate 1 (Main)");
            v3 = visitorRepository.save(v3);

            visitorActivityRepository.save(new VisitorActivity(
                    v3,
                    "REQUESTED",
                    guard,
                    "Walk-in technician at Gate 1. Sent approval notification to Flat 1203."
            ));
        }

        // 4. Past checked-out cab for Aamir Shaikh (T21-1601)
        if (flat3 != null && aamir != null) {
            Visitor v4 = new Visitor(
                    "VP-102948",
                    "tok_uber_cab_1029",
                    flat3,
                    aamir,
                    "Uber Cab (Suresh Kadam)",
                    "+919822077199",
                    VisitorType.CAB,
                    "Airport drop pickup",
                    "MH12-TC-9001",
                    VisitorStatus.CHECKED_OUT,
                    now.minusHours(4),
                    now.minusHours(2),
                    true
            );
            v4.setCheckInTime(now.minusHours(3).minusMinutes(45));
            v4.setCheckOutTime(now.minusHours(3).minusMinutes(20));
            v4.setEntryGuard(guard);
            v4.setExitGuard(guard);
            v4.setEntryGate("Gate 1 (Main)");
            v4.setExitGate("Gate 2 (East)");
            v4 = visitorRepository.save(v4);

            visitorActivityRepository.save(new VisitorActivity(
                    v4,
                    "PRE_APPROVED",
                    aamir,
                    "Resident pre-approved airport cab pass."
            ));
            visitorActivityRepository.save(new VisitorActivity(
                    v4,
                    "CHECKED_IN",
                    guard,
                    "Cab checked in at Gate 1."
            ));
            visitorActivityRepository.save(new VisitorActivity(
                    v4,
                    "CHECKED_OUT",
                    guard,
                    "Cab checked out at Gate 2 (East)."
            ));
        }

        log.info("Seeded initial sample visitor records with pre-approval, walk-in, active, and exit states.");
    }

    private void seedAmenities() {
        if (amenityRepository.count() > 0) {
            log.info("Amenities already seeded. Skipping.");
            return;
        }

        Amenity gym = new Amenity(
                "FitLife Fitness Gym",
                "GYM",
                AmenityCategory.FITNESS,
                "State-of-the-art gym equipped with commercial treadmills, ellipticals, free weights, resistance machines, and dedicated crossfit functional training zone.",
                "Clubhouse, Level 1",
                java.time.LocalTime.of(6, 0),
                java.time.LocalTime.of(22, 0),
                60,
                30,
                2,
                2,
                "1. Clean indoor sports shoes and personal towels are mandatory at all times.\n2. Please sanitize workout benches and dumbbells after use.\n3. Return all weights to their respective racks.\n4. Children below 14 must be accompanied by an adult."
        );

        Amenity pool = new Amenity(
                "Olympic Swimming Pool",
                "POOL",
                AmenityCategory.FITNESS,
                "Temperature-regulated 50m Olympic-length swimming pool featuring dedicated lap lanes, an adjoining toddler splash pool, and certified on-duty lifeguards.",
                "Podium Level, Central Courtyard",
                java.time.LocalTime.of(6, 0),
                java.time.LocalTime.of(21, 0),
                60,
                25,
                2,
                2,
                "1. Proper nylon or lycra swimwear and swim caps are strictly mandatory.\n2. Mandatory shower before entering the pool.\n3. Strictly no glass bottles, food, or footwear on the pool deck.\n4. Children must remain under parental supervision."
        );

        Amenity clubhouse = new Amenity(
                "Grand Vista Clubhouse Lounge",
                "CLUBHOUSE",
                AmenityCategory.LEISURE,
                "Elegant residents social lounge featuring plush seating, indoor board games, snooker table, and a peaceful reading library overlooking the central gardens.",
                "Central Amenity Hub, Ground Floor",
                java.time.LocalTime.of(8, 0),
                java.time.LocalTime.of(22, 0),
                120,
                15,
                1,
                4,
                "1. Maintain quiet library decorum; headphones mandatory for mobile audio.\n2. Outside cooked food and carbonated drinks are strictly prohibited.\n3. Handle snooker equipment and library books with care."
        );

        Amenity badminton = new Amenity(
                "Indoor Badminton Courts",
                "BADMINTON",
                AmenityCategory.SPORTS,
                "Dual BWF-standard indoor wooden badminton courts featuring high-bay LED sports lighting and viewing gallery.",
                "Sports Complex, Ground Floor",
                java.time.LocalTime.of(6, 0),
                java.time.LocalTime.of(22, 0),
                60,
                4,
                2,
                2,
                "1. Non-marking gum rubber sole badminton shoes are mandatory. Black soles or running shoes are strictly prohibited.\n2. Players must bring their own racquets and shuttles.\n3. Maximum 4 active players per court."
        );

        Amenity communityHall = new Amenity(
                "Royal Community Banquet Hall",
                "COMMUNITY_HALL",
                AmenityCategory.EVENTS,
                "Spacious air-conditioned banquet hall with theatrical stage, green rooms, and dedicated catering pantry, suitable for society gatherings and resident private celebrations.",
                "Banquet Wing, Ground Level",
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(23, 0),
                240,
                1,
                1,
                24,
                "1. Sound systems and live music permitted strictly until 10:00 PM per Pune municipal bylaws.\n2. Flammable decorations and firecrackers are strictly banned.\n3. Flat owners are liable for event cleanup and waste segregation."
        );

        amenityRepository.save(gym);
        amenityRepository.save(pool);
        amenityRepository.save(clubhouse);
        amenityRepository.save(badminton);
        amenityRepository.save(communityHall);

        log.info("Seeded 5 core township amenities with operating hours, capacity rules, and guidelines.");
    }

    private void seedSampleBookings() {
        if (amenityBookingRepository.count() > 0) {
            log.info("Sample amenity bookings already seeded. Skipping.");
            return;
        }

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate inTwoDays = LocalDate.now().plusDays(2);

        User neeraj = userRepository.findByEmail("neeraj.kulkarni@grandvistapune.in").orElse(null);
        User swati = userRepository.findByEmail("swati.joshi@grandvistapune.in").orElse(null);
        User aamir = userRepository.findByEmail("aamir.shaikh@grandvistapune.in").orElse(null);

        Flat flatT07 = flatRepository.findByTowerIdAndFlatNumber("T07", "1203").orElse(null);
        Flat flatT12 = flatRepository.findByTowerIdAndFlatNumber("T12", "0408").orElse(null);
        Flat flatT21 = flatRepository.findByTowerIdAndFlatNumber("T21", "1601").orElse(null);

        Amenity gym = amenityRepository.findByCode("GYM").orElse(null);
        Amenity pool = amenityRepository.findByCode("POOL").orElse(null);
        Amenity badminton = amenityRepository.findByCode("BADMINTON").orElse(null);
        Amenity communityHall = amenityRepository.findByCode("COMMUNITY_HALL").orElse(null);

        if (neeraj != null && flatT07 != null && badminton != null) {
            amenityBookingRepository.save(new AmenityBooking(
                    "BK-782194",
                    badminton,
                    flatT07,
                    neeraj,
                    tomorrow,
                    java.time.LocalTime.of(7, 0),
                    java.time.LocalTime.of(8, 0),
                    2,
                    "Morning Doubles Practice"
            ));
        }

        if (neeraj != null && flatT07 != null && gym != null) {
            amenityBookingRepository.save(new AmenityBooking(
                    "BK-439120",
                    gym,
                    flatT07,
                    neeraj,
                    tomorrow,
                    java.time.LocalTime.of(18, 0),
                    java.time.LocalTime.of(19, 0),
                    1,
                    "Cardio and strength training session"
            ));
        }

        if (swati != null && flatT12 != null && pool != null) {
            amenityBookingRepository.save(new AmenityBooking(
                    "BK-519842",
                    pool,
                    flatT12,
                    swati,
                    tomorrow,
                    java.time.LocalTime.of(8, 0),
                    java.time.LocalTime.of(9, 0),
                    1,
                    "Lap swimming workout"
            ));
        }

        if (aamir != null && flatT21 != null && communityHall != null) {
            amenityBookingRepository.save(new AmenityBooking(
                    "BK-928371",
                    communityHall,
                    flatT21,
                    aamir,
                    inTwoDays,
                    java.time.LocalTime.of(13, 0),
                    java.time.LocalTime.of(17, 0),
                    1,
                    "Family Anniversary Celebration"
            ));
        }

        log.info("Seeded initial sample amenity bookings for resident demonstration.");
    }

    private void seedNotices() {
        if (noticeRepository.count() > 0) {
            log.info("Notices already seeded. Skipping.");
            return;
        }

        User helpDesk = userRepository.findByEmail("helpdesk@grandvistapune.in").orElse(null);
        Tower towerT07 = towerRepository.findById("T07").orElse(null);
        Tower towerT12 = towerRepository.findById("T12").orElse(null);

        LocalDateTime now = LocalDateTime.now();

        // 1. Emergency Notice (Township-wide, Pinned, Urgent)
        Notice n1 = new Notice(
                "URGENT: Emergency Water Pipeline Repair on Central Feeder Line",
                "Due to an unforeseen rupture in the central groundwater feeder line near Podium Block B, water supply will be temporarily throttled from 01:00 PM to 05:00 PM today across all towers. Emergency overhead tank reserves are currently engaged. Please conserve water for essential domestic usage only. Maintenance emergency squads are actively on-site.",
                NoticeCategory.EMERGENCY,
                NoticePriority.URGENT,
                NoticeStatus.PUBLISHED,
                null, // Township-wide
                true, // Pinned
                now.minusHours(2),
                now.plusDays(2),
                helpDesk
        );

        // 2. Tower-Specific Maintenance Notice (Tower T07 Iris, Pinned, High)
        Notice n2 = new Notice(
                "Tower G (Iris / T07): Passenger Lift #2 Scheduled Bi-Monthly Maintenance",
                "Please note that Passenger Elevator Lift #2 in Tower G will remain shut down tomorrow between 10:00 AM and 02:00 PM for mandatory safety brake calibration and rope tension testing by Otis certified engineers. Service Lift #1 and Passenger Lift #3 will remain fully operational during this window. We regret the temporary inconvenience.",
                NoticeCategory.MAINTENANCE,
                NoticePriority.HIGH,
                NoticeStatus.PUBLISHED,
                towerT07, // Tower T07 specific
                true, // Pinned
                now.minusHours(5),
                now.plusDays(3),
                helpDesk
        );

        // 3. Billing Notice (Township-wide, Normal)
        Notice n3 = new Notice(
                "Society Maintenance Dues Reminder for Quarter 4 (FY 2026-27)",
                "Dear Flat Owners and Residents, the monthly society maintenance bills for the ongoing quarter are due for reconciliation. Residents can make immediate test payments via the online portal using UPI or net banking mock modes. Kindly ensure clearing all outstanding dues before the 15th to avoid late fees as per society bye-laws.",
                NoticeCategory.BILLING,
                NoticePriority.NORMAL,
                NoticeStatus.PUBLISHED,
                null, // Township-wide
                false,
                now.minusDays(1),
                now.plusDays(14),
                helpDesk
        );

        // 4. Cultural & Event Notice (Township-wide, Normal)
        Notice n4 = new Notice(
                "Grand Vista Annual Diwali Cultural Mela & Deepotsav Celebration",
                "Apex Facility Management and the Township Resident Welfare Committee invite all 2,400 families across 28 towers to our Grand Vista Diwali Carnival at the Central Amphitheatre on Saturday at 06:30 PM. Stalls featuring regional cuisine, kids gaming arena, rangoli exhibition, and classical musical performances. Green fireworks only permitted in designated podium courts.",
                NoticeCategory.EVENT,
                NoticePriority.NORMAL,
                NoticeStatus.PUBLISHED,
                null, // Township-wide
                false,
                now.minusDays(2),
                now.plusDays(10),
                helpDesk
        );

        // 5. Tower-Specific Notice (Tower T12 Magnolia, Low)
        Notice n5 = new Notice(
                "Tower L (Magnolia / T12): Mandatory Fire Safety Evacuation Drill",
                "In coordination with the Pune City Fire Brigade, Tower L will conduct a mock smoke and high-rise evacuation drill this Sunday at 11:00 AM. Emergency alarm sirens will sound for 3 minutes. Residents are requested not to panic and participate by using stairwells instead of elevators.",
                NoticeCategory.GENERAL,
                NoticePriority.LOW,
                NoticeStatus.PUBLISHED,
                towerT12, // Tower T12 specific
                false,
                now.minusDays(3),
                now.plusDays(5),
                helpDesk
        );

        noticeRepository.save(n1);
        noticeRepository.save(n2);
        noticeRepository.save(n3);
        noticeRepository.save(n4);
        noticeRepository.save(n5);

        log.info("Seeded 5 initial township and tower-specific notices with emergency, maintenance, and event categories.");
    }
}
