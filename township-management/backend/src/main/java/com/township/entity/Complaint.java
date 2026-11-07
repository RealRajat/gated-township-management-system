package com.township.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "complaint_number", nullable = false, unique = true, length = 50)
    private String complaintNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flat_id", nullable = false)
    private Flat flat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id", nullable = false)
    private User resident;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ComplaintCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ComplaintPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ComplaintStatus status = ComplaintStatus.SUBMITTED;

    @Column(name = "is_critical_safety_escalation", nullable = false)
    private boolean isCriticalSafetyEscalation = false;

    @Column(name = "safety_trigger_keyword", length = 100)
    private String safetyTriggerKeyword;

    @Column(name = "ai_suggested_category", length = 50)
    private String aiSuggestedCategory;

    @Column(name = "ai_suggested_priority", length = 50)
    private String aiSuggestedPriority;

    @Column(name = "ai_confidence_score")
    private Integer aiConfidenceScore;

    @Column(name = "assigned_team", length = 100)
    private String assignedTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_staff_id")
    private User assignedStaff;

    @Column(name = "photo_storage_path", length = 500)
    private String photoStoragePath;

    @Column(name = "photo_original_filename", length = 255)
    private String photoOriginalFilename;

    @Column(name = "photo_content_type", length = 100)
    private String photoContentType;

    @Column(name = "photo_size_bytes")
    private Long photoSizeBytes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "response_deadline")
    private LocalDateTime responseDeadline;

    @Column(name = "resolution_deadline")
    private LocalDateTime resolutionDeadline;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "resident_rating")
    private Integer residentRating;

    @Column(name = "resident_feedback", columnDefinition = "TEXT")
    private String residentFeedback;

    @Column(name = "rated_at")
    private LocalDateTime ratedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_duplicate_of_id")
    private Complaint parentDuplicateOf;

    public Complaint() {}

    public Complaint(
            String complaintNumber,
            Flat flat,
            User resident,
            String title,
            String description,
            ComplaintCategory category,
            ComplaintPriority priority,
            boolean isCriticalSafetyEscalation,
            String safetyTriggerKeyword,
            String aiSuggestedCategory,
            String aiSuggestedPriority,
            Integer aiConfidenceScore,
            LocalDateTime responseDeadline,
            LocalDateTime resolutionDeadline) {
        this.complaintNumber = complaintNumber;
        this.flat = flat;
        this.resident = resident;
        this.title = title;
        this.description = description;
        this.category = category;
        this.priority = priority;
        this.isCriticalSafetyEscalation = isCriticalSafetyEscalation;
        this.safetyTriggerKeyword = safetyTriggerKeyword;
        this.aiSuggestedCategory = aiSuggestedCategory;
        this.aiSuggestedPriority = aiSuggestedPriority;
        this.aiConfidenceScore = aiConfidenceScore;
        this.responseDeadline = responseDeadline;
        this.resolutionDeadline = resolutionDeadline;
        this.status = ComplaintStatus.SUBMITTED;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getComplaintNumber() {
        return complaintNumber;
    }

    public void setComplaintNumber(String complaintNumber) {
        this.complaintNumber = complaintNumber;
    }

    public Flat getFlat() {
        return flat;
    }

    public void setFlat(Flat flat) {
        this.flat = flat;
    }

    public User getResident() {
        return resident;
    }

    public void setResident(User resident) {
        this.resident = resident;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ComplaintCategory getCategory() {
        return category;
    }

    public void setCategory(ComplaintCategory category) {
        this.category = category;
    }

    public ComplaintPriority getPriority() {
        return priority;
    }

    public void setPriority(ComplaintPriority priority) {
        this.priority = priority;
    }

    public ComplaintStatus getStatus() {
        return status;
    }

    public void setStatus(ComplaintStatus status) {
        this.status = status;
    }

    public boolean isCriticalSafetyEscalation() {
        return isCriticalSafetyEscalation;
    }

    public void setCriticalSafetyEscalation(boolean criticalSafetyEscalation) {
        isCriticalSafetyEscalation = criticalSafetyEscalation;
    }

    public String getSafetyTriggerKeyword() {
        return safetyTriggerKeyword;
    }

    public void setSafetyTriggerKeyword(String safetyTriggerKeyword) {
        this.safetyTriggerKeyword = safetyTriggerKeyword;
    }

    public String getAiSuggestedCategory() {
        return aiSuggestedCategory;
    }

    public void setAiSuggestedCategory(String aiSuggestedCategory) {
        this.aiSuggestedCategory = aiSuggestedCategory;
    }

    public String getAiSuggestedPriority() {
        return aiSuggestedPriority;
    }

    public void setAiSuggestedPriority(String aiSuggestedPriority) {
        this.aiSuggestedPriority = aiSuggestedPriority;
    }

    public Integer getAiConfidenceScore() {
        return aiConfidenceScore;
    }

    public void setAiConfidenceScore(Integer aiConfidenceScore) {
        this.aiConfidenceScore = aiConfidenceScore;
    }

    public String getAssignedTeam() {
        return assignedTeam;
    }

    public void setAssignedTeam(String assignedTeam) {
        this.assignedTeam = assignedTeam;
    }

    public User getAssignedStaff() {
        return assignedStaff;
    }

    public void setAssignedStaff(User assignedStaff) {
        this.assignedStaff = assignedStaff;
    }

    public String getPhotoStoragePath() {
        return photoStoragePath;
    }

    public void setPhotoStoragePath(String photoStoragePath) {
        this.photoStoragePath = photoStoragePath;
    }

    public String getPhotoOriginalFilename() {
        return photoOriginalFilename;
    }

    public void setPhotoOriginalFilename(String photoOriginalFilename) {
        this.photoOriginalFilename = photoOriginalFilename;
    }

    public String getPhotoContentType() {
        return photoContentType;
    }

    public void setPhotoContentType(String photoContentType) {
        this.photoContentType = photoContentType;
    }

    public Long getPhotoSizeBytes() {
        return photoSizeBytes;
    }

    public void setPhotoSizeBytes(Long photoSizeBytes) {
        this.photoSizeBytes = photoSizeBytes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getResponseDeadline() {
        return responseDeadline;
    }

    public void setResponseDeadline(LocalDateTime responseDeadline) {
        this.responseDeadline = responseDeadline;
    }

    public LocalDateTime getResolutionDeadline() {
        return resolutionDeadline;
    }

    public void setResolutionDeadline(LocalDateTime resolutionDeadline) {
        this.resolutionDeadline = resolutionDeadline;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public Integer getResidentRating() {
        return residentRating;
    }

    public void setResidentRating(Integer residentRating) {
        this.residentRating = residentRating;
    }

    public String getResidentFeedback() {
        return residentFeedback;
    }

    public void setResidentFeedback(String residentFeedback) {
        this.residentFeedback = residentFeedback;
    }

    public LocalDateTime getRatedAt() {
        return ratedAt;
    }

    public void setRatedAt(LocalDateTime ratedAt) {
        this.ratedAt = ratedAt;
    }

    public Complaint getParentDuplicateOf() {
        return parentDuplicateOf;
    }

    public void setParentDuplicateOf(Complaint parentDuplicateOf) {
        this.parentDuplicateOf = parentDuplicateOf;
    }
}

