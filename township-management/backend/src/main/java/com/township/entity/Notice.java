package com.township.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "notices",
    indexes = {
        @Index(name = "idx_notices_status", columnList = "status"),
        @Index(name = "idx_notices_tower", columnList = "target_tower_id"),
        @Index(name = "idx_notices_priority", columnList = "priority"),
        @Index(name = "idx_notices_pinned_date", columnList = "is_pinned, publish_date DESC"),
        @Index(name = "idx_notices_category", columnList = "category"),
        @Index(name = "idx_notices_expiry", columnList = "expiry_date")
    }
)
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NoticeCategory category = NoticeCategory.GENERAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NoticePriority priority = NoticePriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NoticeStatus status = NoticeStatus.PUBLISHED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_tower_id")
    private Tower targetTower; // Nullable: NULL = Township-Wide

    @Column(name = "is_pinned", nullable = false)
    private Boolean isPinned = false;

    @Column(name = "publish_date", nullable = false)
    private LocalDateTime publishDate;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Notice() {
    }

    public Notice(String title, String description, NoticeCategory category, NoticePriority priority,
                  NoticeStatus status, Tower targetTower, Boolean isPinned, LocalDateTime publishDate,
                  LocalDateTime expiryDate, User author) {
        this.title = title;
        this.description = description;
        this.category = category != null ? category : NoticeCategory.GENERAL;
        this.priority = priority != null ? priority : NoticePriority.NORMAL;
        this.status = status != null ? status : NoticeStatus.PUBLISHED;
        this.targetTower = targetTower;
        this.isPinned = isPinned != null ? isPinned : false;
        this.publishDate = publishDate != null ? publishDate : LocalDateTime.now();
        this.expiryDate = expiryDate;
        this.author = author;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.publishDate == null) {
            this.publishDate = now;
        }
        if (this.isPinned == null) {
            this.isPinned = false;
        }
        if (this.status == null) {
            this.status = NoticeStatus.PUBLISHED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public NoticeCategory getCategory() {
        return category;
    }

    public void setCategory(NoticeCategory category) {
        this.category = category;
    }

    public NoticePriority getPriority() {
        return priority;
    }

    public void setPriority(NoticePriority priority) {
        this.priority = priority;
    }

    public NoticeStatus getStatus() {
        return status;
    }

    public void setStatus(NoticeStatus status) {
        this.status = status;
    }

    public Tower getTargetTower() {
        return targetTower;
    }

    public void setTargetTower(Tower targetTower) {
        this.targetTower = targetTower;
    }

    public Boolean getIsPinned() {
        return isPinned;
    }

    public void setIsPinned(Boolean pinned) {
        isPinned = pinned;
    }

    public LocalDateTime getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(LocalDateTime publishDate) {
        this.publishDate = publishDate;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

