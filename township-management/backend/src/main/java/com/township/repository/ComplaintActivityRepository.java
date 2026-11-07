package com.township.repository;

import com.township.entity.ComplaintActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintActivityRepository extends JpaRepository<ComplaintActivity, Long> {

    List<ComplaintActivity> findByComplaintIdOrderByTimestampAsc(Long complaintId);

    List<ComplaintActivity> findByComplaintIdAndIsInternalOnlyFalseOrderByTimestampAsc(Long complaintId);
}

