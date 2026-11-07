package com.township.service;

import com.township.dto.*;
import com.township.entity.*;
import com.township.repository.*;
import com.township.service.ai.ComplaintClassificationService;
import com.township.service.storage.PhotoStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ComplaintService {

    private static final Logger log = LoggerFactory.getLogger(ComplaintService.class);

    private final ComplaintRepository complaintRepository;
    private final ComplaintActivityRepository activityRepository;
    private final FlatRepository flatRepository;
    private final UserRepository userRepository;
    private final FlatMemberRepository flatMemberRepository;
    private final ComplaintClassificationService classificationService;
    private final PhotoStorageService photoStorageService;

    public ComplaintService(
            ComplaintRepository complaintRepository,
            ComplaintActivityRepository activityRepository,
            FlatRepository flatRepository,
            UserRepository userRepository,
            FlatMemberRepository flatMemberRepository,
            ComplaintClassificationService classificationService,
            PhotoStorageService photoStorageService) {
        this.complaintRepository = complaintRepository;
        this.activityRepository = activityRepository;
        this.flatRepository = flatRepository;
        this.userRepository = userRepository;
        this.flatMemberRepository = flatMemberRepository;
        this.classificationService = classificationService;
        this.photoStorageService = photoStorageService;
    }

    /**
     * Preview AI and Safety Classification before filing.
     */
    public ComplaintClassificationDto previewClassification(ComplaintPreviewRequest request) {
        return classificationService.classify(request.title(), request.description());
    }

    /**
     * File a new resident complaint with optional photo.
     */
    @Transactional
    public ComplaintDetailDto createComplaint(CreateComplaintRequest request, MultipartFile photo, String userEmail) {
        User resident = getUserByEmail(userEmail);
        Flat flat = flatRepository.findById(request.flatId())
                .orElseThrow(() -> new IllegalArgumentException("Apartment flat not found with ID: " + request.flatId()));

        // Security check: Resident must be an authorized member of the flat (Help Desk can log on behalf)
        validateFlatAccess(flat.getId(), resident);

        // Run AI & Deterministic Safety Classification
        ComplaintClassificationDto classification = classificationService.classify(request.title(), request.description());

        // Category determination: user provided or AI suggested
        ComplaintCategory category = request.category() != null
                ? request.category()
                : (classification.suggestedCategory() != null ? classification.suggestedCategory() : ComplaintCategory.OTHER);

        // Priority determination: SAFETY KEYWORDS OVERRIDE UNCONDITIONALLY
        ComplaintPriority priority;
        boolean isSafety = classification.isSafetyEscalation();
        String safetyKeyword = classification.safetyTriggerKeyword();

        if (isSafety) {
            priority = ComplaintPriority.CRITICAL; // Mandatory Safety Override
            log.warn("Complaint marked CRITICAL due to safety keyword '{}'. AI downgrades prevented.", safetyKeyword);
        } else if (request.priority() != null) {
            priority = request.priority();
        } else {
            priority = classification.suggestedPriority() != null ? classification.suggestedPriority() : ComplaintPriority.MEDIUM;
        }

        // Calculate Response and Resolution SLA Deadlines
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime responseDeadline = calculateResponseDeadline(now, priority);
        LocalDateTime resolutionDeadline = calculateResolutionDeadline(now, priority);

        // Generate unique complaint number
        String complaintNumber = generateUniqueComplaintNumber();

        Complaint complaint = new Complaint(
                complaintNumber,
                flat,
                resident,
                request.title().trim(),
                request.description().trim(),
                category,
                priority,
                isSafety,
                safetyKeyword,
                classification.suggestedCategory() != null ? classification.suggestedCategory().name() : null,
                classification.suggestedPriority() != null ? classification.suggestedPriority().name() : null,
                classification.confidenceScore(),
                responseDeadline,
                resolutionDeadline
        );

        // Save optional photo attachment
        if (photo != null && !photo.isEmpty()) {
            var storedPhoto = photoStorageService.storePhoto(photo);
            if (storedPhoto != null) {
                complaint.setPhotoStoragePath(storedPhoto.storagePath());
                complaint.setPhotoOriginalFilename(storedPhoto.originalFilename());
                complaint.setPhotoContentType(storedPhoto.contentType());
                complaint.setPhotoSizeBytes(storedPhoto.sizeBytes());
            }
        }

        complaint = complaintRepository.save(complaint);

        // Record Initial Creation Activity
        String creationNote = isSafety
                ? "Complaint filed and CRITICALLY ESCALATED: Safety trigger keyword detected (" + safetyKeyword + ")."
                : "Complaint successfully logged by resident.";

        ComplaintActivity activity = new ComplaintActivity(
                complaint,
                resident,
                resident.getFullName(),
                resident.getRole().name(),
                null,
                ComplaintStatus.SUBMITTED,
                isSafety ? "SAFETY_ESCALATED" : "CREATED",
                creationNote,
                "Auto-triaged by " + classification.provider() + " (Confidence: " + classification.confidenceScore() + "%)",
                false
        );
        activityRepository.save(activity);

        return convertToDetailDto(complaint, resident.getRole() == Role.HELP_DESK);
    }

    /**
     * Get resident's complaints
     */
    @Transactional(readOnly = true)
    public List<ComplaintSummaryDto> getResidentComplaints(String userEmail) {
        User resident = getUserByEmail(userEmail);
        List<FlatMember> memberships = flatMemberRepository.findByUserId(resident.getId());
        if (memberships.isEmpty()) {
            return complaintRepository.findByResidentIdOrderByCreatedAtDesc(resident.getId())
                    .stream().map(this::convertToSummaryDto).toList();
        }

        List<Long> flatIds = memberships.stream().map(m -> m.getFlat().getId()).toList();
        return complaintRepository.findByFlatIdInOrderByCreatedAtDesc(flatIds)
                .stream().map(this::convertToSummaryDto).toList();
    }

    /**
     * Get complaint details by ID (with security permission check)
     */
    @Transactional(readOnly = true)
    public ComplaintDetailDto getComplaintById(Long complaintId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found with ID: " + complaintId));

        boolean isHelpDesk = user.getRole() == Role.HELP_DESK;
        if (!isHelpDesk) {
            validateFlatAccess(complaint.getFlat().getId(), user);
        }

        return convertToDetailDto(complaint, isHelpDesk);
    }

    /**
     * Secure photo retrieval with ownership check
     */
    @Transactional(readOnly = true)
    public Resource getComplaintPhoto(Long complaintId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found with ID: " + complaintId));

        if (user.getRole() != Role.HELP_DESK) {
            validateFlatAccess(complaint.getFlat().getId(), user);
        }

        if (complaint.getPhotoStoragePath() == null) {
            throw new IllegalArgumentException("This complaint does not have an attached photo.");
        }

        return photoStorageService.loadPhotoAsResource(complaint.getPhotoStoragePath());
    }

    /**
     * Help Desk: view all complaints with search and filtering
     */
    @Transactional(readOnly = true)
    public List<ComplaintSummaryDto> getAllComplaintsForHelpDesk(
            ComplaintStatus status,
            ComplaintCategory category,
            ComplaintPriority priority,
            String towerId,
            Boolean overdueOnly) {

        List<Complaint> list = complaintRepository.findAllByOrderByCreatedAtDesc();
        LocalDateTime now = LocalDateTime.now();

        return list.stream()
                .filter(c -> status == null || c.getStatus() == status)
                .filter(c -> category == null || c.getCategory() == category)
                .filter(c -> priority == null || c.getPriority() == priority)
                .filter(c -> towerId == null || towerId.isBlank() || c.getFlat().getTower().getId().equalsIgnoreCase(towerId.trim()))
                .filter(c -> {
                    if (overdueOnly != null && overdueOnly) {
                        return c.getResolutionDeadline() != null &&
                                c.getResolutionDeadline().isBefore(now) &&
                                c.getStatus() != ComplaintStatus.RESOLVED &&
                                c.getStatus() != ComplaintStatus.CLOSED;
                    }
                    return true;
                })
                .map(this::convertToSummaryDto)
                .toList();
    }

    /**
     * Help Desk: Assign complaint to team or staff member
     */
    @Transactional
    public ComplaintDetailDto assignComplaint(Long complaintId, AssignComplaintRequest request, String staffEmail) {
        User staff = getUserByEmail(staffEmail);
        requireHelpDesk(staff);

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found with ID: " + complaintId));

        User assignedStaffMember = null;
        if (request.assignedStaffId() != null) {
            assignedStaffMember = userRepository.findById(request.assignedStaffId()).orElse(null);
        }

        complaint.setAssignedTeam(request.assignedTeam().trim());
        complaint.setAssignedStaff(assignedStaffMember);

        ComplaintStatus oldStatus = complaint.getStatus();
        if (oldStatus == ComplaintStatus.SUBMITTED) {
            complaint.setStatus(ComplaintStatus.ASSIGNED);
            if (complaint.getAcknowledgedAt() == null) {
                complaint.setAcknowledgedAt(LocalDateTime.now());
            }
        }

        complaint = complaintRepository.save(complaint);

        String note = "Assigned to team '" + request.assignedTeam() + "'" +
                (assignedStaffMember != null ? " (Technician: " + assignedStaffMember.getFullName() + ")" : "") +
                (request.notes() != null && !request.notes().isBlank() ? ". Notes: " + request.notes().trim() : "");

        ComplaintActivity activity = new ComplaintActivity(
                complaint,
                staff,
                staff.getFullName(),
                staff.getRole().name(),
                oldStatus,
                complaint.getStatus(),
                "ASSIGNED",
                note,
                request.notes(),
                false
        );
        activityRepository.save(activity);

        return convertToDetailDto(complaint, true);
    }

    /**
     * Help Desk: Update status, priority, and resolution notes
     */
    @Transactional
    public ComplaintDetailDto updateComplaintStatus(Long complaintId, UpdateComplaintStatusRequest request, String staffEmail) {
        User staff = getUserByEmail(staffEmail);
        requireHelpDesk(staff);

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found with ID: " + complaintId));

        ComplaintStatus oldStatus = complaint.getStatus();
        ComplaintStatus newStatus = request.newStatus();

        complaint.setStatus(newStatus);
        LocalDateTime now = LocalDateTime.now();

        if (newStatus == ComplaintStatus.ASSIGNED && complaint.getAcknowledgedAt() == null) {
            complaint.setAcknowledgedAt(now);
        } else if (newStatus == ComplaintStatus.RESOLVED) {
            complaint.setResolvedAt(now);
            if (request.resolutionNotes() != null && !request.resolutionNotes().isBlank()) {
                complaint.setResolutionNotes(request.resolutionNotes().trim());
            }
        } else if (newStatus == ComplaintStatus.CLOSED) {
            complaint.setClosedAt(now);
        }

        complaint = complaintRepository.save(complaint);

        String publicNote = "Status updated from " + oldStatus + " to " + newStatus + "." +
                (request.resolutionNotes() != null && !request.resolutionNotes().isBlank() ? " Resolution: " + request.resolutionNotes().trim() : "");

        ComplaintActivity activity = new ComplaintActivity(
                complaint,
                staff,
                staff.getFullName(),
                staff.getRole().name(),
                oldStatus,
                newStatus,
                "STATUS_CHANGED",
                publicNote,
                request.internalNotes(),
                false
        );
        activityRepository.save(activity);

        return convertToDetailDto(complaint, true);
    }

    /**
     * Help Desk: Add internal note visible only to staff
     */
    @Transactional
    public ComplaintDetailDto addInternalNote(Long complaintId, AddInternalNoteRequest request, String staffEmail) {
        User staff = getUserByEmail(staffEmail);
        requireHelpDesk(staff);

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found with ID: " + complaintId));

        ComplaintActivity activity = new ComplaintActivity(
                complaint,
                staff,
                staff.getFullName(),
                staff.getRole().name(),
                complaint.getStatus(),
                complaint.getStatus(),
                "INTERNAL_NOTE",
                null,
                request.internalNote().trim(),
                true
        );
        activityRepository.save(activity);

        return convertToDetailDto(complaint, true);
    }

    /**
     * Help Desk: Link duplicate complaint without closing or deleting it
     */
    @Transactional
    public ComplaintDetailDto linkDuplicate(Long complaintId, LinkDuplicateRequest request, String staffEmail) {
        User staff = getUserByEmail(staffEmail);
        requireHelpDesk(staff);

        Complaint duplicate = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Duplicate complaint not found with ID: " + complaintId));

        Complaint master = complaintRepository.findById(request.masterComplaintId())
                .orElseThrow(() -> new IllegalArgumentException("Master complaint not found with ID: " + request.masterComplaintId()));

        if (duplicate.getId().equals(master.getId())) {
            throw new IllegalArgumentException("A complaint cannot be linked to itself as a duplicate.");
        }

        duplicate.setParentDuplicateOf(master);
        duplicate = complaintRepository.save(duplicate);

        String note = "Linked as related/duplicate to master ticket #" + master.getComplaintNumber() + "." +
                (request.notes() != null && !request.notes().isBlank() ? " Note: " + request.notes().trim() : "");

        ComplaintActivity activity = new ComplaintActivity(
                duplicate,
                staff,
                staff.getFullName(),
                staff.getRole().name(),
                duplicate.getStatus(),
                duplicate.getStatus(),
                "DUPLICATE_LINKED",
                note,
                request.notes(),
                false
        );
        activityRepository.save(activity);

        return convertToDetailDto(duplicate, true);
    }

    /**
     * Resident: Rate complaint after resolution (1 to 5 stars)
     */
    @Transactional
    public ComplaintDetailDto rateComplaint(Long complaintId, RateComplaintRequest request, String userEmail) {
        User resident = getUserByEmail(userEmail);
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found with ID: " + complaintId));

        // Validate resident ownership
        validateFlatAccess(complaint.getFlat().getId(), resident);

        // Rating is permitted only after resolution
        if (complaint.getStatus() != ComplaintStatus.RESOLVED && complaint.getStatus() != ComplaintStatus.CLOSED) {
            throw new IllegalStateException("Complaints can only be rated after resolution. Current status: " + complaint.getStatus());
        }

        complaint.setResidentRating(request.rating());
        complaint.setResidentFeedback(request.feedback() != null ? request.feedback().trim() : null);
        complaint.setRatedAt(LocalDateTime.now());
        complaint = complaintRepository.save(complaint);

        String note = "Resident submitted " + request.rating() + "-star rating." +
                (request.feedback() != null && !request.feedback().isBlank() ? " Feedback: " + request.feedback() : "");

        ComplaintActivity activity = new ComplaintActivity(
                complaint,
                resident,
                resident.getFullName(),
                resident.getRole().name(),
                complaint.getStatus(),
                complaint.getStatus(),
                "RATED",
                note,
                null,
                false
        );
        activityRepository.save(activity);

        return convertToDetailDto(complaint, false);
    }

    /**
     * Help Desk KPI metrics
     */
    @Transactional(readOnly = true)
    public ComplaintOverviewKpiDto getHelpDeskOverview() {
        LocalDateTime now = LocalDateTime.now();
        List<Complaint> all = complaintRepository.findAll();

        long total = all.size();
        long open = all.stream().filter(c -> c.getStatus() != ComplaintStatus.RESOLVED && c.getStatus() != ComplaintStatus.CLOSED).count();
        long critical = all.stream().filter(Complaint::isCriticalSafetyEscalation).count();
        long overdue = all.stream().filter(c -> c.getResolutionDeadline() != null &&
                c.getResolutionDeadline().isBefore(now) &&
                c.getStatus() != ComplaintStatus.RESOLVED &&
                c.getStatus() != ComplaintStatus.CLOSED).count();

        LocalDate today = LocalDate.now();
        long resolvedToday = all.stream().filter(c -> c.getResolvedAt() != null && c.getResolvedAt().toLocalDate().isEqual(today)).count();

        OptionalDouble avg = all.stream().filter(c -> c.getResidentRating() != null).mapToInt(Complaint::getResidentRating).average();
        double averageRating = avg.isPresent() ? Math.round(avg.getAsDouble() * 10.0) / 10.0 : 0.0;

        return new ComplaintOverviewKpiDto(total, open, critical, overdue, resolvedToday, averageRating);
    }

    // --- Helper Methods ---

    private void validateFlatAccess(Long flatId, User user) {
        if (user.getRole() == Role.HELP_DESK) return;
        boolean hasAccess = flatMemberRepository.existsByFlatIdAndUserId(flatId, user.getId());
        if (!hasAccess) {
            throw new AccessDeniedException("Access denied: You are not authorized to view or file complaints for apartment flat ID: " + flatId);
        }
    }

    private void requireHelpDesk(User user) {
        if (user.getRole() != Role.HELP_DESK) {
            throw new AccessDeniedException("Access denied: Facility Management Help Desk clearance required.");
        }
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
    }

    private String generateUniqueComplaintNumber() {
        String prefix = "CMP-" + DateTimeFormatter.ofPattern("yyyyMM").format(LocalDate.now()) + "-";
        for (int i = 0; i < 20; i++) {
            String candidate = prefix + String.format("%04d", (int)(Math.random() * 8999 + 1000));
            if (!complaintRepository.existsByComplaintNumber(candidate)) {
                return candidate;
            }
        }
        return prefix + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private LocalDateTime calculateResponseDeadline(LocalDateTime created, ComplaintPriority priority) {
        return switch (priority) {
            case CRITICAL -> created.plusHours(1);
            case HIGH -> created.plusHours(4);
            case MEDIUM -> created.plusHours(12);
            case LOW -> created.plusHours(24);
        };
    }

    private LocalDateTime calculateResolutionDeadline(LocalDateTime created, ComplaintPriority priority) {
        return switch (priority) {
            case CRITICAL -> created.plusHours(4);
            case HIGH -> created.plusHours(12);
            case MEDIUM -> created.plusHours(24);
            case LOW -> created.plusHours(48);
        };
    }

    private ComplaintSummaryDto convertToSummaryDto(Complaint c) {
        boolean isOverdue = c.getResolutionDeadline() != null &&
                c.getResolutionDeadline().isBefore(LocalDateTime.now()) &&
                c.getStatus() != ComplaintStatus.RESOLVED &&
                c.getStatus() != ComplaintStatus.CLOSED;

        return new ComplaintSummaryDto(
                c.getId(),
                c.getComplaintNumber(),
                c.getFlat().getId(),
                c.getFlat().getTower().getId(),
                c.getFlat().getTower().getName(),
                c.getFlat().getFlatNumber(),
                c.getResident() != null ? c.getResident().getFullName() : "Resident",
                c.getResident() != null ? c.getResident().getPhone() : null,
                c.getTitle(),
                c.getCategory(),
                c.getPriority(),
                c.getStatus(),
                c.isCriticalSafetyEscalation(),
                c.getSafetyTriggerKeyword(),
                c.getAssignedTeam(),
                c.getAssignedStaff() != null ? c.getAssignedStaff().getFullName() : null,
                c.getPhotoStoragePath() != null,
                c.getCreatedAt(),
                c.getResponseDeadline(),
                c.getResolutionDeadline(),
                isOverdue,
                c.getResidentRating(),
                c.getParentDuplicateOf() != null ? c.getParentDuplicateOf().getId() : null,
                c.getParentDuplicateOf() != null ? c.getParentDuplicateOf().getComplaintNumber() : null
        );
    }

    private ComplaintDetailDto convertToDetailDto(Complaint c, boolean isHelpDesk) {
        boolean isOverdue = c.getResolutionDeadline() != null &&
                c.getResolutionDeadline().isBefore(LocalDateTime.now()) &&
                c.getStatus() != ComplaintStatus.RESOLVED &&
                c.getStatus() != ComplaintStatus.CLOSED;

        // Fetch activities, filtering internal notes if user is resident
        List<ComplaintActivity> rawActivities = isHelpDesk
                ? activityRepository.findByComplaintIdOrderByTimestampAsc(c.getId())
                : activityRepository.findByComplaintIdAndIsInternalOnlyFalseOrderByTimestampAsc(c.getId());

        List<ComplaintActivityDto> activityDtos = rawActivities.stream()
                .map(a -> new ComplaintActivityDto(
                        a.getId(),
                        a.getActorName(),
                        a.getActorRole(),
                        a.getOldStatus(),
                        a.getNewStatus(),
                        a.getActivityType(),
                        a.getPublicNote(),
                        isHelpDesk ? a.getInternalNote() : null, // Strict internal note hiding
                        a.isInternalOnly(),
                        a.getTimestamp()
                )).toList();

        // Linked duplicates
        List<ComplaintSummaryDto> linkedDuplicates = Collections.emptyList();

        // Potential duplicates for Help Desk triage
        List<ComplaintSummaryDto> potentialDuplicates = Collections.emptyList();
        if (isHelpDesk && c.getStatus() != ComplaintStatus.CLOSED) {
            List<Complaint> potential = complaintRepository.findPotentialTowerDuplicates(c.getFlat().getTower().getId(), c.getCategory());
            potentialDuplicates = potential.stream()
                    .filter(p -> !p.getId().equals(c.getId()))
                    .map(this::convertToSummaryDto)
                    .toList();
        }

        String photoUrl = c.getPhotoStoragePath() != null ? "/api/complaints/" + c.getId() + "/photo" : null;

        return new ComplaintDetailDto(
                c.getId(),
                c.getComplaintNumber(),
                c.getFlat().getId(),
                c.getFlat().getTower().getId(),
                c.getFlat().getTower().getName(),
                c.getFlat().getFlatNumber(),
                c.getResident() != null ? c.getResident().getFullName() : "Resident",
                c.getResident() != null ? c.getResident().getEmail() : null,
                c.getResident() != null ? c.getResident().getPhone() : null,
                c.getTitle(),
                c.getDescription(),
                c.getCategory(),
                c.getPriority(),
                c.getStatus(),
                c.isCriticalSafetyEscalation(),
                c.getSafetyTriggerKeyword(),
                c.getAiSuggestedCategory(),
                c.getAiSuggestedPriority(),
                c.getAiConfidenceScore(),
                c.getAssignedTeam(),
                c.getAssignedStaff() != null ? c.getAssignedStaff().getId() : null,
                c.getAssignedStaff() != null ? c.getAssignedStaff().getFullName() : null,
                c.getPhotoStoragePath() != null,
                c.getPhotoOriginalFilename(),
                c.getPhotoSizeBytes(),
                photoUrl,
                c.getCreatedAt(),
                c.getResponseDeadline(),
                c.getResolutionDeadline(),
                isOverdue,
                c.getAcknowledgedAt(),
                c.getResolvedAt(),
                c.getClosedAt(),
                c.getResolutionNotes(),
                c.getResidentRating(),
                c.getResidentFeedback(),
                c.getRatedAt(),
                c.getParentDuplicateOf() != null ? c.getParentDuplicateOf().getId() : null,
                c.getParentDuplicateOf() != null ? c.getParentDuplicateOf().getComplaintNumber() : null,
                activityDtos,
                linkedDuplicates,
                potentialDuplicates
        );
    }
}
