package com.township.repository;

import com.township.entity.Tower;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TowerRepository extends JpaRepository<Tower, String> {
    List<Tower> findAllByOrderByIdAsc();
}
