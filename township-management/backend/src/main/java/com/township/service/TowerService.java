package com.township.service;

import com.township.dto.FlatSummaryDto;
import com.township.dto.TowerDto;
import com.township.entity.Tower;
import com.township.repository.FlatRepository;
import com.township.repository.TowerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TowerService {

    private final TowerRepository towerRepository;
    private final FlatRepository flatRepository;

    public TowerService(TowerRepository towerRepository, FlatRepository flatRepository) {
        this.towerRepository = towerRepository;
        this.flatRepository = flatRepository;
    }

    @Transactional(readOnly = true)
    public List<TowerDto> getAllTowers() {
        return towerRepository.findAllByOrderByIdAsc().stream()
                .map(t -> new TowerDto(t.getId(), t.getName(), t.getFloors(), t.getTotalUnits(), t.getType()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FlatSummaryDto> getFlatsByTower(String towerId) {
        return flatRepository.findByTowerIdOrderByFlatNumberAsc(towerId).stream()
                .map(f -> new FlatSummaryDto(
                        f.getId(),
                        f.getTower().getId(),
                        f.getTower().getName(),
                        f.getFlatNumber(),
                        f.getFloorNumber(),
                        f.getAreaSqFt(),
                        f.getOccupancyStatus(),
                        "UNASSIGNED"
                ))
                .toList();
    }
}
