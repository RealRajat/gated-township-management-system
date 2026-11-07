package com.township.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaint_activities")
public class ComplaintActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(name = "actor_name", length = 100)
    private String actorName;

    @Column(name = "actor_role", length = 50)
    private String actorRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 50)
    private ComplaintStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 50)
    private ComplaintStatus newStatus;

    @Column(name = "activity_type", nullable = false, length = 50)
    private String activityType;

    @Column(name = "public_note", columnDefinition = "TEXT")
    private String publicNote;

    @Column(name = "internal_note", columnDefinition = "TEXT")
    private String internalNote;

    @Column(name = "is_internal_only", nullable = false)
    private boolean isInternalOnly = false;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    public ComplaintActivity() {}

    public ComplaintActivity(
            Complaint complaint,
            User actor,
            String actorName,
            String actorRole,
            ComplaintStatus oldStatus,
            ComplaintStatus newStatus,
            String activityType,
            String publicNote,
            String internalNote,
            boolean isInternalOnly) {
        this.complaint = complaint;
        this.actor = actor;
        this.actorName = actorName;
        this.actorRole = actorRole;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.activityType = activityType;
        this.publicNote = publicNote;
        this.internalNote = internalNote;
        this.isInternalOnly = isInternalOnly;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Complaint getComplaint() {
        return complaint;
    }

    public void setComplaint(Complaint complaint) {
        this.complaint = complaint;
    }

    public User getActor() {
        return actor;
    }

    public void setActor(User actor) {
        this.actor = actor;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public String getActorRole() {
        return actorRole;
    }

    public void setActorRole(String actorRole) {
        this.actorRole = actorRole;
    }

    public ComplaintStatus getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(ComplaintStatus oldStatus) {
        this.oldStatus = oldStatus;
    }

    public ComplaintStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(ComplaintStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getPublicNote() {
        return publicNote;
    }

    public void setPublicNote(String publicNote) {
        this.publicNote = publicNote;
    }

    public String getInternalNote() {
        return internalNote;
    }

    public void setInternalNote(String internalNote) {
        this.internalNote = internalNote;
    }

    public boolean isInternalOnly() {
        return isInternalOnly;
    }

    public void setInternalOnly(boolean internalOnly) {
        isInternalOnly = internalOnly;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

