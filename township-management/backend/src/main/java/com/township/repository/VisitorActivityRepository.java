package com.township.repository;

import com.township.entity.VisitorActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VisitorActivityRepository extends JpaRepository<VisitorActivity, Long> {

    List<VisitorActivity> findByVisitorIdOrderByTimestampAsc(Long visitorId);
}

