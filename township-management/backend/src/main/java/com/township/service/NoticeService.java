package com.township.service;

import com.township.dto.*;
import com.township.entity.*;
import com.township.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NoticeService {

    private static final Logger log = LoggerFactory.getLogger(NoticeService.class);

    private final NoticeRepository noticeRepository;
    private final NoticeReadRepository noticeReadRepository;
    private final TowerRepository towerRepository;
    private final FlatMemberRepository flatMemberRepository;
    private final UserRepository userRepository;

    public NoticeService(
            NoticeRepository noticeRepository,
            NoticeReadRepository noticeReadRepository,
            TowerRepository towerRepository,
            FlatMemberRepository flatMemberRepository,
            UserRepository userRepository) {
        this.noticeRepository = noticeRepository;
        this.noticeReadRepository = noticeReadRepository;
        this.towerRepository = towerRepository;
        this.flatMemberRepository = flatMemberRepository;
        this.userRepository = userRepository;
    }

    // =========================================================================
    // RESIDENT NOTICE EXPERIENCE & READ TRACKING
    // =========================================================================

    @Transactional(readOnly = true)
    public List<NoticeDto> getResidentNotices(String username, boolean includeExpired, NoticeCategory category) {
        User user = getUserByUsername(username);
        Set<String> userTowerIds = getUserTowerIds(user);

        LocalDateTime now = LocalDateTime.now();
        List<Notice> notices = noticeRepository.findPublishedNoticesForTowers(
                userTowerIds.isEmpty() ? Collections.singleton("__NONE__") : userTowerIds,
                now,
                includeExpired,
                category
        );

        Set<Long> readNoticeIds = noticeReadRepository.findReadNoticeIdsByUserId(user.getId());

        return notices.stream()
                .map(n -> toNoticeDto(n, readNoticeIds.contains(n.getId()), null))
                .collect(Collectors.toList());
    }

    @Transactional
    public NoticeDto getNoticeByIdAndMarkRead(Long id, String username) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notice not found with id: " + id));

        User user = getUserByUsername(username);

        // Security check for residents: must be township-wide OR targeted to user's tower
        if (user.getRole() == Role.RESIDENT) {
            Set<String> userTowerIds = getUserTowerIds(user);
            if (notice.getTargetTower() != null && !userTowerIds.contains(notice.getTargetTower().getId())) {
                throw new AccessDeniedException("Forbidden: You do not have access to notices for other towers.");
            }
        }

        // Auto-mark as read idempotently
        boolean alreadyRead = noticeReadRepository.existsByNoticeIdAndUserId(notice.getId(), user.getId());
        LocalDateTime readAt = null;
        if (!alreadyRead) {
            NoticeRead readEntry = new NoticeRead(notice, user);
            NoticeRead saved = noticeReadRepository.save(readEntry);
            readAt = saved.getReadAt();
        } else {
            readAt = noticeReadRepository.findByNoticeIdAndUserId(notice.getId(), user.getId())
                    .map(NoticeRead::getReadAt)
                    .orElse(LocalDateTime.now());
        }

        long readCount = noticeReadRepository.countByNoticeId(notice.getId());
        return toNoticeDto(notice, true, readAt, readCount);
    }

    @Transactional
    public void markAsRead(Long id, String username) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notice not found with id: " + id));

        User user = getUserByUsername(username);

        // Security check for residents
        if (user.getRole() == Role.RESIDENT) {
            Set<String> userTowerIds = getUserTowerIds(user);
            if (notice.getTargetTower() != null && !userTowerIds.contains(notice.getTargetTower().getId())) {
                throw new AccessDeniedException("Forbidden: You do not have access to notices for other towers.");
            }
        }

        if (!noticeReadRepository.existsByNoticeIdAndUserId(notice.getId(), user.getId())) {
            noticeReadRepository.save(new NoticeRead(notice, user));
        }
    }

    @Transactional
    public void markAllAsRead(String username) {
        User user = getUserByUsername(username);
        Set<String> userTowerIds = getUserTowerIds(user);

        List<Notice> visibleNotices = noticeRepository.findPublishedNoticesForTowers(
                userTowerIds.isEmpty() ? Collections.singleton("__NONE__") : userTowerIds,
                LocalDateTime.now(),
                false,
                null
        );

        Set<Long> alreadyReadIds = noticeReadRepository.findReadNoticeIdsByUserId(user.getId());

        List<NoticeRead> newReads = visibleNotices.stream()
                .filter(n -> !alreadyReadIds.contains(n.getId()))
                .map(n -> new NoticeRead(n, user))
                .collect(Collectors.toList());

        if (!newReads.isEmpty()) {
            noticeReadRepository.saveAll(newReads);
            log.info("User {} marked {} notices as read.", username, newReads.size());
        }
    }

    // =========================================================================
    // HELP DESK ADMINISTRATIVE MANAGEMENT
    // =========================================================================

    @Transactional
    public NoticeDto createNotice(CreateNoticeRequest request, String adminUsername) {
        User author = getUserByUsername(adminUsername);

        Tower targetTower = null;
        if (request.targetTowerId() != null && !request.targetTowerId().trim().isEmpty() && !"ALL".equalsIgnoreCase(request.targetTowerId())) {
            targetTower = towerRepository.findById(request.targetTowerId().trim().toUpperCase())
                    .orElseThrow(() -> new IllegalArgumentException("Target tower not found: " + request.targetTowerId()));
        }

        LocalDateTime publishDate = request.publishDate() != null ? request.publishDate() : LocalDateTime.now();
        if (request.expiryDate() != null && request.expiryDate().isBefore(publishDate)) {
            throw new IllegalArgumentException("Notice expiry date must be after the publish date.");
        }

        Notice notice = new Notice(
                request.title().trim(),
                request.description().trim(),
                request.category(),
                request.priority(),
                request.status() != null ? request.status() : NoticeStatus.PUBLISHED,
                targetTower,
                request.isPinned() != null ? request.isPinned() : false,
                publishDate,
                request.expiryDate(),
                author
        );

        Notice saved = noticeRepository.save(notice);
        log.info("Help Desk created notice: id={}, title='{}', tower={}",
                saved.getId(), saved.getTitle(), targetTower != null ? targetTower.getId() : "ALL");

        return toNoticeDto(saved, false, null, 0);
    }

    @Transactional
    public NoticeDto updateNotice(Long id, UpdateNoticeRequest request, String adminUsername) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notice not found with id: " + id));

        Tower targetTower = null;
        if (request.targetTowerId() != null && !request.targetTowerId().trim().isEmpty() && !"ALL".equalsIgnoreCase(request.targetTowerId())) {
            targetTower = towerRepository.findById(request.targetTowerId().trim().toUpperCase())
                    .orElseThrow(() -> new IllegalArgumentException("Target tower not found: " + request.targetTowerId()));
        }

        LocalDateTime publishDate = request.publishDate() != null ? request.publishDate() : notice.getPublishDate();
        if (request.expiryDate() != null && request.expiryDate().isBefore(publishDate)) {
            throw new IllegalArgumentException("Notice expiry date must be after the publish date.");
        }

        notice.setTitle(request.title().trim());
        notice.setDescription(request.description().trim());
        notice.setCategory(request.category());
        notice.setPriority(request.priority());
        notice.setStatus(request.status());
        notice.setTargetTower(targetTower);
        if (request.isPinned() != null) {
            notice.setIsPinned(request.isPinned());
        }
        notice.setPublishDate(publishDate);
        notice.setExpiryDate(request.expiryDate());

        Notice saved = noticeRepository.save(notice);
        log.info("Help Desk updated notice: id={}, title='{}'", saved.getId(), saved.getTitle());

        long readCount = noticeReadRepository.countByNoticeId(saved.getId());
        return toNoticeDto(saved, false, null, readCount);
    }

    @Transactional
    public NoticeDto togglePin(Long id, String adminUsername) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notice not found with id: " + id));

        notice.setIsPinned(!Boolean.TRUE.equals(notice.getIsPinned()));
        Notice saved = noticeRepository.save(notice);
        log.info("Help Desk toggled pin on notice: id={}, isPinned={}", saved.getId(), saved.getIsPinned());

        long readCount = noticeReadRepository.countByNoticeId(saved.getId());
        return toNoticeDto(saved, false, null, readCount);
    }

    @Transactional
    public NoticeDto archiveNotice(Long id, String adminUsername) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notice not found with id: " + id));

        notice.setStatus(NoticeStatus.ARCHIVED);
        Notice saved = noticeRepository.save(notice);
        log.info("Help Desk archived notice: id={}", saved.getId());

        long readCount = noticeReadRepository.countByNoticeId(saved.getId());
        return toNoticeDto(saved, false, null, readCount);
    }

    @Transactional
    public void deleteNotice(Long id, String adminUsername) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notice not found with id: " + id));

        noticeReadRepository.deleteByNoticeId(notice.getId());
        noticeRepository.delete(notice);
        log.info("Help Desk deleted notice: id={}", id);
    }

    @Transactional(readOnly = true)
    public List<NoticeDto> getAllNoticesAdmin(NoticeStatus status, NoticeCategory category, NoticePriority priority, String towerId) {
        String filterTower = (towerId != null && !towerId.isBlank() && !"ALL".equalsIgnoreCase(towerId)) ? towerId : null;
        List<Notice> notices = noticeRepository.searchNoticesAdmin(status, category, priority, filterTower);

        return notices.stream()
                .map(n -> {
                    long readCount = noticeReadRepository.countByNoticeId(n.getId());
                    return toNoticeDto(n, false, null, readCount);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NoticeOverviewKpiDto getOverviewKpis(String username) {
        long totalNotices = noticeRepository.count();
        long activePublished = noticeRepository.countByStatus(NoticeStatus.PUBLISHED);
        long emergencyNotices = noticeRepository.countByCategoryAndStatus(NoticeCategory.EMERGENCY, NoticeStatus.PUBLISHED);
        long pinnedNotices = noticeRepository.countByIsPinnedTrueAndStatus(NoticeStatus.PUBLISHED);

        // Count unread for this user
        User user = getUserByUsername(username);
        Set<String> userTowerIds = getUserTowerIds(user);
        List<Notice> visible = noticeRepository.findPublishedNoticesForTowers(
                userTowerIds.isEmpty() ? Collections.singleton("__NONE__") : userTowerIds,
                LocalDateTime.now(),
                false,
                null
        );
        Set<Long> readIds = noticeReadRepository.findReadNoticeIdsByUserId(user.getId());
        long unreadCount = visible.stream().filter(n -> !readIds.contains(n.getId())).count();

        return new NoticeOverviewKpiDto(
                totalNotices,
                activePublished,
                emergencyNotices,
                pinnedNotices,
                unreadCount
        );
    }

    // =========================================================================
    // HELPER METHODS & DTO MAPPERS
    // =========================================================================

    private Set<String> getUserTowerIds(User user) {
        if (user.getRole() == Role.HELP_DESK || user.getRole() == Role.GUARD) {
            // Staff have visibility across all towers
            return towerRepository.findAll().stream().map(Tower::getId).collect(Collectors.toSet());
        }

        List<FlatMember> members = flatMemberRepository.findByUserId(user.getId());
        return members.stream()
                .map(m -> m.getFlat().getTower().getId())
                .collect(Collectors.toSet());
    }

    private User getUserByUsername(String username) {
        return userRepository.findByEmail(username)
                .or(() -> userRepository.findByPhone(username))
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    private NoticeDto toNoticeDto(Notice n, boolean isRead, LocalDateTime readAt) {
        long readCount = noticeReadRepository.countByNoticeId(n.getId());
        return toNoticeDto(n, isRead, readAt, readCount);
    }

    private NoticeDto toNoticeDto(Notice n, boolean isRead, LocalDateTime readAt, long readCount) {
        Tower tower = n.getTargetTower();
        User author = n.getAuthor();
        boolean isExpired = n.getExpiryDate() != null && n.getExpiryDate().isBefore(LocalDateTime.now());

        return new NoticeDto(
                n.getId(),
                n.getTitle(),
                n.getDescription(),
                n.getCategory(),
                n.getPriority(),
                n.getStatus(),
                tower != null ? tower.getId() : null,
                tower != null ? tower.getName() : null,
                tower == null,
                n.getIsPinned(),
                n.getPublishDate(),
                n.getExpiryDate(),
                author != null ? author.getFullName() : "Help Desk Administration",
                isRead,
                readAt,
                readCount,
                isExpired,
                n.getCreatedAt(),
                n.getUpdatedAt()
        );
    }
}

