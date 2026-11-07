package com.township.service;

import com.township.dto.*;
import com.township.entity.*;
import com.township.repository.*;
import com.township.service.storage.VisitorPhotoStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class VisitorService {

    private static final Logger log = LoggerFactory.getLogger(VisitorService.class);
    private static final String PASS_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final VisitorRepository visitorRepository;
    private final VisitorActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final FlatRepository flatRepository;
    private final FlatMemberRepository flatMemberRepository;
    private final VisitorPhotoStorageService photoStorageService;

    @Value("${township.visitor.photo.retention.days:90}")
    private int photoRetentionDays = 90;

    public VisitorService(
            VisitorRepository visitorRepository,
            VisitorActivityRepository activityRepository,
            UserRepository userRepository,
            FlatRepository flatRepository,
            FlatMemberRepository flatMemberRepository,
            VisitorPhotoStorageService photoStorageService) {
        this.visitorRepository = visitorRepository;
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
        this.flatRepository = flatRepository;
        this.flatMemberRepository = flatMemberRepository;
        this.photoStorageService = photoStorageService;
    }

    /**
     * Resident: Pre-approve an upcoming visitor.
     */
    @Transactional
    public VisitorDto preApproveVisitor(PreApproveVisitorRequest request, String residentIdentifier) {
        User resident = getUserByEmailOrPhone(residentIdentifier);

        // Security Check: Resident must belong to the specified flat
        if (!flatMemberRepository.existsByFlatIdAndUserId(request.flatId(), resident.getId())) {
            throw new AccessDeniedException("Forbidden: You are not a registered member of this flat.");
        }

        Flat flat = flatRepository.findById(request.flatId())
                .orElseThrow(() -> new IllegalArgumentException("Flat not found: " + request.flatId()));

        if (request.validUntil().isBefore(request.validFrom())) {
            throw new IllegalArgumentException("Pass validity expiry must be after start time.");
        }

        String passCode = generateUniquePassCode();
        String passToken = UUID.randomUUID().toString().replace("-", "");

        boolean isOneTime = request.isOneTime() == null || request.isOneTime();

        Visitor visitor = new Visitor(
                passCode,
                passToken,
                flat,
                resident,
                request.visitorName().trim(),
                request.visitorPhone().trim(),
                request.visitorType(),
                request.purpose() != null ? request.purpose().trim() : null,
                request.vehicleNumber() != null ? request.vehicleNumber().trim() : null,
                VisitorStatus.PRE_APPROVED,
                request.validFrom(),
                request.validUntil(),
                isOneTime
        );

        visitor = visitorRepository.save(visitor);

        logActivity(visitor, "PRE_APPROVED", resident,
                String.format("Pre-approved %s pass for %s (Valid until %s)",
                        request.visitorType(), request.visitorName(), request.validUntil()));

        log.info("Pre-approved visitor pass created: passCode={}, resident={}, flat={}",
                passCode, resident.getEmail(), flat.getFlatNumber());

        return convertToDto(visitor);
    }

    /**
     * Resident: Revoke an unused pre-approved or pending pass.
     */
    @Transactional
    public VisitorDto revokePass(Long visitorId, String residentIdentifier) {
        User resident = getUserByEmailOrPhone(residentIdentifier);
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new IllegalArgumentException("Visitor pass not found: " + visitorId));

        // Security Check: Resident must belong to the visitor's flat
        if (!flatMemberRepository.existsByFlatIdAndUserId(visitor.getFlat().getId(), resident.getId())) {
            throw new AccessDeniedException("Forbidden: You can only revoke passes for your own flat.");
        }

        if (visitor.getStatus() == VisitorStatus.CHECKED_IN) {
            throw new IllegalStateException("Cannot revoke pass: Visitor is currently checked in on premises.");
        }

        if (visitor.getStatus() == VisitorStatus.REVOKED) {
            return convertToDto(visitor);
        }

        visitor.setStatus(VisitorStatus.REVOKED);
        visitor = visitorRepository.save(visitor);

        logActivity(visitor, "REVOKED", resident, "Visitor pass revoked by resident.");
        log.info("Visitor pass {} revoked by resident {}", visitor.getPassCode(), resident.getEmail());

        return convertToDto(visitor);
    }

    /**
     * Resident: View all visitors associated with resident's flats.
     */
    @Transactional(readOnly = true)
    public List<VisitorDto> getMyVisitors(String residentIdentifier, VisitorStatus statusFilter) {
        User resident = getUserByEmailOrPhone(residentIdentifier);
        List<Long> flatIds = flatMemberRepository.findFlatIdsByUserId(resident.getId());

        if (flatIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Visitor> visitors;
        if (statusFilter != null) {
            visitors = visitorRepository.findByFlatIdInAndStatusOrderByCreatedAtDesc(flatIds, statusFilter);
        } else {
            visitors = visitorRepository.findByFlatIdInOrderByCreatedAtDesc(flatIds);
        }

        return visitors.stream().map(this::convertToDto).toList();
    }

    /**
     * Resident: Get pending approval requests awaiting resident action.
     */
    @Transactional(readOnly = true)
    public List<VisitorDto> getMyPendingApprovals(String residentIdentifier) {
        return getMyVisitors(residentIdentifier, VisitorStatus.PENDING_APPROVAL);
    }

    /**
     * Resident: Approve or Reject a walk-in visitor request.
     * GUARDS ARE STRICTLY PREVENTED FROM CALLING THIS METHOD.
     */
    @Transactional
    public VisitorDto processResidentApproval(Long visitorId, ResidentApprovalDecisionRequest request, String residentIdentifier) {
        User resident = getUserByEmailOrPhone(residentIdentifier);

        // Security Check: Only residents can approve visitor entries
        if (resident.getRole() == Role.GUARD) {
            throw new AccessDeniedException("Security Violation: Gate Guards cannot approve visitor requests on behalf of residents.");
        }

        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new IllegalArgumentException("Visitor request not found: " + visitorId));

        // Security Check: Resident must belong to the requested flat
        if (!flatMemberRepository.existsByFlatIdAndUserId(visitor.getFlat().getId(), resident.getId())) {
            throw new AccessDeniedException("Forbidden: You cannot approve visitors for a flat you do not belong to.");
        }

        if (visitor.getStatus() != VisitorStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Visitor request is not in PENDING_APPROVAL status. Current status: " + visitor.getStatus());
        }

        if (Boolean.TRUE.equals(request.approved())) {
            visitor.setStatus(VisitorStatus.APPROVED);
            visitor.setApprovedBy(resident);
            visitor.setApprovedAt(LocalDateTime.now());
            // Valid for immediate entry (e.g. next 2 hours)
            visitor.setValidFrom(LocalDateTime.now().minusMinutes(5));
            visitor.setValidUntil(LocalDateTime.now().plusHours(2));

            logActivity(visitor, "APPROVED", resident,
                    "Visitor entry request approved by resident " + resident.getFullName());
            log.info("Visitor request {} approved by resident {}", visitor.getPassCode(), resident.getEmail());
        } else {
            visitor.setStatus(VisitorStatus.REJECTED);
            visitor.setRejectionReason(request.rejectionReason() != null ? request.rejectionReason().trim() : "Rejected by resident");

            logActivity(visitor, "REJECTED", resident,
                    "Visitor entry rejected by resident: " + visitor.getRejectionReason());
            log.info("Visitor request {} rejected by resident {}", visitor.getPassCode(), resident.getEmail());
        }

        visitor = visitorRepository.save(visitor);
        return convertToDto(visitor);
    }

    /**
     * Gate Guard: Register unapproved walk-in visitor, triggering resident approval request.
     */
    @Transactional
    public VisitorDto registerWalkInVisitor(WalkInVisitorRequest request, MultipartFile photo, String guardIdentifier) {
        User guard = getUserByEmailOrPhone(guardIdentifier);
        if (guard.getRole() != Role.GUARD && guard.getRole() != Role.HELP_DESK) {
            throw new AccessDeniedException("Only gate guards and help desk staff can log walk-in visitors.");
        }

        Flat flat = flatRepository.findById(request.flatId())
                .orElseThrow(() -> new IllegalArgumentException("Destination flat not found: " + request.flatId()));

        String passCode = generateUniquePassCode();
        String passToken = UUID.randomUUID().toString().replace("-", "");

        LocalDateTime now = LocalDateTime.now();

        Visitor visitor = new Visitor(
                passCode,
                passToken,
                flat,
                null, // No resident author for walk-in
                request.visitorName().trim(),
                request.visitorPhone().trim(),
                request.visitorType(),
                request.purpose() != null ? request.purpose().trim() : null,
                request.vehicleNumber() != null ? request.vehicleNumber().trim() : null,
                VisitorStatus.PENDING_APPROVAL,
                now,
                now.plusHours(2),
                true
        );

        if (request.gate() != null && !request.gate().isBlank()) {
            visitor.setEntryGate(request.gate().trim());
        }

        // Store photo if captured at gate
        if (photo != null && !photo.isEmpty()) {
            var photoInfo = photoStorageService.storePhoto(photo);
            visitor.setPhotoStoragePath(photoInfo.storagePath());
            visitor.setPhotoOriginalFilename(photoInfo.originalFilename());
            visitor.setPhotoSizeBytes(photoInfo.sizeBytes());
            visitor.setPhotoCapturedAt(now);
        }

        visitor = visitorRepository.save(visitor);

        logActivity(visitor, "REQUESTED", guard,
                String.format("Walk-in %s registered at %s by Guard %s. Awaiting resident approval.",
                        request.visitorType(), visitor.getEntryGate(), guard.getFullName()));

        log.info("Walk-in visitor registered: passCode={}, flat={}, phone={}",
                passCode, flat.getFlatNumber(), request.visitorPhone());

        return convertToDto(visitor);
    }

    /**
     * Gate Guard: Lookup visitor by pass code, mobile number, or QR token.
     */
    @Transactional(readOnly = true)
    public List<VisitorDto> lookupVisitor(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        List<Visitor> list = visitorRepository.lookupByPassCodeOrPhoneOrToken(query.trim());
        return list.stream().map(this::convertToDto).toList();
    }

    /**
     * Gate Guard: Check-In verified visitor.
     */
    @Transactional
    public VisitorDto checkInVisitor(GateCheckInRequest request, MultipartFile photo, String guardIdentifier) {
        User guard = getUserByEmailOrPhone(guardIdentifier);
        if (guard.getRole() != Role.GUARD && guard.getRole() != Role.HELP_DESK) {
            throw new AccessDeniedException("Only gate guards can record visitor check-ins.");
        }

        String lookup = request.passCodeOrToken().trim();
        Visitor visitor = visitorRepository.findByPassCode(lookup)
                .or(() -> visitorRepository.findByPassToken(lookup))
                .orElseThrow(() -> new IllegalArgumentException("No visitor pass found matching: " + lookup));

        validateForCheckIn(visitor);

        LocalDateTime now = LocalDateTime.now();
        visitor.setStatus(VisitorStatus.CHECKED_IN);
        visitor.setCheckInTime(now);
        visitor.setEntryGuard(guard);
        if (request.gate() != null && !request.gate().isBlank()) {
            visitor.setEntryGate(request.gate().trim());
        }
        if (request.vehicleNumber() != null && !request.vehicleNumber().isBlank()) {
            visitor.setVehicleNumber(request.vehicleNumber().trim());
        }

        // Optional gate photo
        if (photo != null && !photo.isEmpty()) {
            var photoInfo = photoStorageService.storePhoto(photo);
            visitor.setPhotoStoragePath(photoInfo.storagePath());
            visitor.setPhotoOriginalFilename(photoInfo.originalFilename());
            visitor.setPhotoSizeBytes(photoInfo.sizeBytes());
            visitor.setPhotoCapturedAt(now);
        }

        visitor = visitorRepository.save(visitor);

        logActivity(visitor, "CHECKED_IN", guard,
                String.format("Visitor checked in at %s by Guard %s. Destination: Flat %s (%s).",
                        visitor.getEntryGate(), guard.getFullName(), visitor.getFlat().getFlatNumber(), visitor.getFlat().getTower().getName()));

        log.info("Visitor checked in: passCode={}, flat={}, guard={}",
                visitor.getPassCode(), visitor.getFlat().getFlatNumber(), guard.getEmail());

        return convertToDto(visitor);
    }

    /**
     * Gate Guard: Check-Out visitor exiting the township.
     */
    @Transactional
    public VisitorDto checkOutVisitor(Long visitorId, GateCheckOutRequest request, String guardIdentifier) {
        User guard = getUserByEmailOrPhone(guardIdentifier);
        if (guard.getRole() != Role.GUARD && guard.getRole() != Role.HELP_DESK) {
            throw new AccessDeniedException("Only gate guards can record visitor check-outs.");
        }

        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new IllegalArgumentException("Visitor record not found: " + visitorId));

        if (visitor.getStatus() != VisitorStatus.CHECKED_IN) {
            throw new IllegalStateException("Cannot check-out: Visitor is not currently checked-in on premises. Current status: " + visitor.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        visitor.setStatus(VisitorStatus.CHECKED_OUT);
        visitor.setCheckOutTime(now);
        visitor.setExitGuard(guard);
        if (request != null && request.gate() != null && !request.gate().isBlank()) {
            visitor.setExitGate(request.gate().trim());
        } else {
            visitor.setExitGate(visitor.getEntryGate());
        }

        visitor = visitorRepository.save(visitor);

        logActivity(visitor, "CHECKED_OUT", guard,
                String.format("Visitor exited through %s, recorded by Guard %s.",
                        visitor.getExitGate(), guard.getFullName()));

        log.info("Visitor checked out: passCode={}, flat={}, guard={}",
                visitor.getPassCode(), visitor.getFlat().getFlatNumber(), guard.getEmail());

        return convertToDto(visitor);
    }

    /**
     * Offline Synchronization: Batch ingest offline check-in and check-out events idempotently.
     */
    @Transactional
    public OfflineSyncBatchResponse syncOfflineEvents(OfflineSyncBatchRequest request, String guardIdentifier) {
        User guard = getUserByEmailOrPhone(guardIdentifier);
        if (guard.getRole() != Role.GUARD && guard.getRole() != Role.HELP_DESK) {
            throw new AccessDeniedException("Only gate guards can synchronize offline gate records.");
        }

        int total = request.events().size();
        int synced = 0;
        int duplicates = 0;
        int failed = 0;
        List<String> messages = new ArrayList<>();

        for (OfflineSyncItemDto event : request.events()) {
            String syncId = event.offlineSyncId().trim();

            // 1. Idempotency Check: Already processed?
            if (visitorRepository.findByOfflineSyncId(syncId).isPresent()) {
                duplicates++;
                messages.add("Skipped duplicate sync event: " + syncId);
                continue;
            }

            // 2. Lookup visitor pass
            String lookup = event.passCodeOrToken().trim();
            Optional<Visitor> optionalVisitor = visitorRepository.findByPassCode(lookup)
                    .or(() -> visitorRepository.findByPassToken(lookup));

            if (optionalVisitor.isEmpty()) {
                failed++;
                messages.add("Sync Failed for " + syncId + ": No visitor found matching " + lookup);
                continue;
            }

            Visitor visitor = optionalVisitor.get();

            try {
                if ("CHECK_IN".equalsIgnoreCase(event.action())) {
                    if (visitor.getStatus() == VisitorStatus.CHECKED_IN) {
                        duplicates++;
                        messages.add("Visitor already checked-in for event " + syncId);
                        continue;
                    }
                    validateForCheckIn(visitor);
                    visitor.setStatus(VisitorStatus.CHECKED_IN);
                    visitor.setCheckInTime(event.timestamp());
                    visitor.setEntryGuard(guard);
                    visitor.setEntryGate(event.gate() != null ? event.gate() : "Gate 1 (Offline)");
                    if (event.vehicleNumber() != null) {
                        visitor.setVehicleNumber(event.vehicleNumber().trim());
                    }
                    visitor.setOfflineSyncId(syncId);
                    visitor.setOfflineSync(true);

                    visitorRepository.save(visitor);
                    logActivity(visitor, "OFFLINE_SYNCED", guard,
                            "Check-in synchronized from offline gate buffer (Offline Time: " + event.timestamp() + ")");
                    synced++;
                    messages.add("Successfully synced offline CHECK_IN for " + visitor.getPassCode());

                } else if ("CHECK_OUT".equalsIgnoreCase(event.action())) {
                    visitor.setStatus(VisitorStatus.CHECKED_OUT);
                    visitor.setCheckOutTime(event.timestamp());
                    visitor.setExitGuard(guard);
                    visitor.setExitGate(event.gate() != null ? event.gate() : "Gate 1 (Offline)");
                    visitor.setOfflineSyncId(syncId);
                    visitor.setOfflineSync(true);

                    visitorRepository.save(visitor);
                    logActivity(visitor, "OFFLINE_SYNCED", guard,
                            "Check-out synchronized from offline gate buffer (Offline Time: " + event.timestamp() + ")");
                    synced++;
                    messages.add("Successfully synced offline CHECK_OUT for " + visitor.getPassCode());
                } else {
                    failed++;
                    messages.add("Unknown sync action '" + event.action() + "' for " + syncId);
                }
            } catch (Exception ex) {
                failed++;
                messages.add("Error syncing " + syncId + ": " + ex.getMessage());
            }
        }

        log.info("Offline sync finished: total={}, synced={}, duplicates={}, failed={}",
                total, synced, duplicates, failed);

        return new OfflineSyncBatchResponse(total, synced, duplicates, failed, messages);
    }

    /**
     * Gate Guard: Download active passes cache for offline gate tolerance.
     */
    @Transactional(readOnly = true)
    public List<GateActivePassDto> getActivePassesForOfflineCache() {
        LocalDateTime now = LocalDateTime.now();
        List<VisitorStatus> activeStatuses = List.of(VisitorStatus.PRE_APPROVED, VisitorStatus.APPROVED);
        List<Visitor> active = visitorRepository.findByStatusInAndValidUntilAfterOrderByValidFromAsc(activeStatuses, now);

        return active.stream().map(v -> new GateActivePassDto(
                v.getPassCode(),
                v.getPassToken(),
                v.getVisitorName(),
                v.getVisitorPhone(),
                v.getVisitorType(),
                v.getFlat().getTower().getName(),
                v.getFlat().getFlatNumber(),
                v.getValidFrom(),
                v.getValidUntil(),
                v.isOneTime(),
                v.getStatus()
        )).toList();
    }

    /**
     * Gate / Help Desk: Get live gate KPI overview.
     */
    @Transactional(readOnly = true)
    public GateOverviewKpiDto getGateOverview() {
        long currentlyInside = visitorRepository.countByStatus(VisitorStatus.CHECKED_IN);
        long todayCheckIns = visitorRepository.countTodayCheckIns(LocalDate.now().atStartOfDay());
        long pendingApprovals = visitorRepository.countByStatus(VisitorStatus.PENDING_APPROVAL);
        long preApproved = visitorRepository.countByStatus(VisitorStatus.PRE_APPROVED);

        return new GateOverviewKpiDto(currentlyInside, todayCheckIns, pendingApprovals, preApproved);
    }

    /**
     * Help Desk / Guard: Get visitor details by ID.
     */
    @Transactional(readOnly = true)
    public VisitorDto getVisitorById(Long id, String userIdentifier) {
        User user = getUserByEmailOrPhone(userIdentifier);
        Visitor visitor = visitorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Visitor not found: " + id));

        // If user is resident, verify flat membership
        if (user.getRole() == Role.RESIDENT) {
            if (!flatMemberRepository.existsByFlatIdAndUserId(visitor.getFlat().getId(), user.getId())) {
                throw new AccessDeniedException("Forbidden: You cannot access visitor records for another flat.");
            }
        }

        return convertToDto(visitor);
    }

    /**
     * Help Desk: Search and filter township-wide visitors.
     */
    @Transactional(readOnly = true)
    public List<VisitorDto> getAllVisitorsForHelpDesk(VisitorStatus status, Long flatId, String towerId) {
        List<Visitor> list = visitorRepository.findWithFilters(status, flatId, towerId);
        return list.stream().map(this::convertToDto).toList();
    }

    /**
     * Stream visitor photo securely with role authorization.
     */
    @Transactional(readOnly = true)
    public Resource getVisitorPhoto(Long visitorId, String userIdentifier) {
        User user = getUserByEmailOrPhone(userIdentifier);
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new IllegalArgumentException("Visitor not found: " + visitorId));

        if (user.getRole() == Role.RESIDENT) {
            if (!flatMemberRepository.existsByFlatIdAndUserId(visitor.getFlat().getId(), user.getId())) {
                throw new AccessDeniedException("Forbidden: Access to visitor photo denied.");
            }
        }

        if (visitor.getPhotoStoragePath() == null || visitor.getPhotoDeletedAt() != null) {
            throw new IllegalArgumentException("Photo not available or has been deleted under the 90-day retention policy.");
        }

        return photoStorageService.loadPhotoAsResource(visitor.getPhotoStoragePath());
    }

    /**
     * 90-Day Retention Policy Cleanup Mechanism.
     * Deletes visitor photos older than configured retention days.
     * Scheduled daily at 02:00 AM, plus callable on-demand.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public int cleanupExpiredVisitorPhotos() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(photoRetentionDays);
        List<Visitor> expired = visitorRepository.findExpiredPhotosForPurge(cutoff);

        int count = 0;
        for (Visitor v : expired) {
            String path = v.getPhotoStoragePath();
            photoStorageService.deletePhoto(path);

            v.setPhotoStoragePath(null);
            v.setPhotoDeletedAt(LocalDateTime.now());
            visitorRepository.save(v);

            logActivity(v, "PHOTO_PURGED", null,
                    String.format("Photo purged under township %d-day privacy retention policy.", photoRetentionDays));
            count++;
        }

        if (count > 0) {
            log.info("Privacy retention purge completed: {} visitor photos removed older than {} days (cutoff: {}).",
                    count, photoRetentionDays, cutoff);
        }
        return count;
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private void validateForCheckIn(Visitor visitor) {
        LocalDateTime now = LocalDateTime.now();

        if (visitor.getStatus() == VisitorStatus.REVOKED) {
            throw new IllegalStateException("Entry Denied: This visitor pass was REVOKED by the resident.");
        }
        if (visitor.getStatus() == VisitorStatus.REJECTED) {
            throw new IllegalStateException("Entry Denied: Resident REJECTED entry for this visitor.");
        }
        if (visitor.getStatus() == VisitorStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Entry Denied: Awaiting resident approval. Resident has not approved yet.");
        }
        if (visitor.getStatus() == VisitorStatus.CHECKED_IN) {
            throw new IllegalStateException("Entry Denied: Visitor is ALREADY CHECKED IN on premises.");
        }
        if (visitor.getStatus() == VisitorStatus.CHECKED_OUT && visitor.isOneTime()) {
            throw new IllegalStateException("Entry Denied: This one-time visitor pass has already been used and exited.");
        }
        if (now.isAfter(visitor.getValidUntil())) {
            visitor.setStatus(VisitorStatus.EXPIRED);
            visitorRepository.save(visitor);
            throw new IllegalStateException("Entry Denied: Visitor pass EXPIRED on " + visitor.getValidUntil());
        }
        if (now.isBefore(visitor.getValidFrom().minusMinutes(30))) {
            throw new IllegalStateException("Entry Denied: Visitor pass not yet valid. Scheduled start: " + visitor.getValidFrom());
        }
    }

    private String generateUniquePassCode() {
        for (int i = 0; i < 20; i++) {
            StringBuilder sb = new StringBuilder("VP-");
            for (int j = 0; j < 6; j++) {
                sb.append(PASS_CODE_CHARS.charAt(RANDOM.nextInt(PASS_CODE_CHARS.length())));
            }
            String candidate = sb.toString();
            if (visitorRepository.findByPassCode(candidate).isEmpty()) {
                return candidate;
            }
        }
        return "VP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private void logActivity(Visitor visitor, String actionType, User performedBy, String details) {
        VisitorActivity activity = new VisitorActivity(visitor, actionType, performedBy, details);
        activityRepository.save(activity);
    }

    private User getUserByEmailOrPhone(String identifier) {
        return userRepository.findByIdentifier(identifier.trim())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + identifier));
    }

    private VisitorDto convertToDto(Visitor v) {
        List<VisitorActivityDto> activities = activityRepository.findByVisitorIdOrderByTimestampAsc(v.getId())
                .stream()
                .map(a -> new VisitorActivityDto(
                        a.getId(),
                        a.getActionType(),
                        a.getPerformedBy() != null ? a.getPerformedBy().getFullName() : "System",
                        a.getDetails(),
                        a.getTimestamp()
                )).toList();

        String photoUrl = v.getPhotoStoragePath() != null ? "/api/visitors/" + v.getId() + "/photo" : null;

        return new VisitorDto(
                v.getId(),
                v.getPassCode(),
                v.getPassToken(),
                v.getFlat().getId(),
                v.getFlat().getTower().getId(),
                v.getFlat().getTower().getName(),
                v.getFlat().getFlatNumber(),
                v.getResident() != null ? v.getResident().getId() : null,
                v.getResident() != null ? v.getResident().getFullName() : null,
                v.getResident() != null ? v.getResident().getPhone() : null,
                v.getVisitorName(),
                v.getVisitorPhone(),
                v.getVisitorType(),
                v.getPurpose(),
                v.getVehicleNumber(),
                v.getStatus(),
                v.getValidFrom(),
                v.getValidUntil(),
                v.isOneTime(),
                v.getCheckInTime(),
                v.getCheckOutTime(),
                v.getEntryGate(),
                v.getExitGate(),
                v.getEntryGuard() != null ? v.getEntryGuard().getFullName() : null,
                v.getExitGuard() != null ? v.getExitGuard().getFullName() : null,
                v.getPhotoStoragePath() != null,
                photoUrl,
                v.getPhotoCapturedAt(),
                v.getApprovedBy() != null ? v.getApprovedBy().getFullName() : null,
                v.getApprovedAt(),
                v.getRejectionReason(),
                v.isOfflineSync(),
                v.getCreatedAt(),
                activities
        );
    }
}

