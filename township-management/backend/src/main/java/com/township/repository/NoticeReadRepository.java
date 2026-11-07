package com.township.repository;

import com.township.entity.NoticeRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface NoticeReadRepository extends JpaRepository<NoticeRead, Long> {

    boolean existsByNoticeIdAndUserId(Long noticeId, Long userId);

    Optional<NoticeRead> findByNoticeIdAndUserId(Long noticeId, Long userId);

    @Query("SELECT nr.notice.id FROM NoticeRead nr WHERE nr.user.id = :userId")
    Set<Long> findReadNoticeIdsByUserId(@Param("userId") Long userId);

    long countByNoticeId(Long noticeId);

    void deleteByNoticeId(Long noticeId);
}

