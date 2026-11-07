package com.township.repository;

import com.township.entity.FlatMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlatMemberRepository extends JpaRepository<FlatMember, Long> {
    List<FlatMember> findByUserId(Long userId);
    List<FlatMember> findByFlatId(Long flatId);
    boolean existsByFlatIdAndUserId(Long flatId, Long userId);

    @Query("SELECT fm.flat.id FROM FlatMember fm WHERE fm.user.id = :userId")
    List<Long> findFlatIdsByUserId(@Param("userId") Long userId);
}
