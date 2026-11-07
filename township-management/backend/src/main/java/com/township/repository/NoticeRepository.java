package com.township.repository;

import com.township.entity.Notice;
import com.township.entity.NoticeCategory;
import com.township.entity.NoticePriority;
import com.township.entity.NoticeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    @Query("SELECT n FROM Notice n WHERE " +
           "n.status = 'PUBLISHED' AND " +
           "n.publishDate <= :now AND " +
           "(:includeExpired = true OR n.expiryDate IS NULL OR n.expiryDate > :now) AND " +
           "(n.targetTower IS NULL OR n.targetTower.id IN :towerIds) AND " +
           "(:category IS NULL OR n.category = :category) " +
           "ORDER BY n.isPinned DESC, n.publishDate DESC")
    List<Notice> findPublishedNoticesForTowers(
            @Param("towerIds") Collection<String> towerIds,
            @Param("now") LocalDateTime now,
            @Param("includeExpired") boolean includeExpired,
            @Param("category") NoticeCategory category);

    @Query("SELECT n FROM Notice n WHERE " +
           "(:status IS NULL OR n.status = :status) AND " +
           "(:category IS NULL OR n.category = :category) AND " +
           "(:priority IS NULL OR n.priority = :priority) AND " +
           "(:towerId IS NULL OR n.targetTower.id = :towerId) " +
           "ORDER BY n.isPinned DESC, n.publishDate DESC")
    List<Notice> searchNoticesAdmin(
            @Param("status") NoticeStatus status,
            @Param("category") NoticeCategory category,
            @Param("priority") NoticePriority priority,
            @Param("towerId") String towerId);

    long countByStatus(NoticeStatus status);

    long countByCategoryAndStatus(NoticeCategory category, NoticeStatus status);

    long countByIsPinnedTrueAndStatus(NoticeStatus status);

    @Query("SELECT COUNT(n) FROM Notice n WHERE n.status = 'PUBLISHED' AND n.expiryDate IS NOT NULL AND n.expiryDate <= :now")
    long countExpiredNotices(@Param("now") LocalDateTime now);
}

