package com.township.controller;

import com.township.dto.FlatSummaryDto;
import com.township.dto.TowerDto;
import com.township.service.TowerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/towers")
public class TowerController {

    private final TowerService towerService;

    public TowerController(TowerService towerService) {
        this.towerService = towerService;
    }

    @GetMapping
    public ResponseEntity<List<TowerDto>> getAllTowers() {
        return ResponseEntity.ok(towerService.getAllTowers());
    }

    @GetMapping("/{towerId}/flats")
    public ResponseEntity<List<FlatSummaryDto>> getFlatsByTower(@PathVariable String towerId) {
        return ResponseEntity.ok(towerService.getFlatsByTower(towerId));
    }
}
