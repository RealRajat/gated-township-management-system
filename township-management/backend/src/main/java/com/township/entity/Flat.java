package com.township.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "flats",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_tower_flat_number", columnNames = {"tower_id", "flat_number"})
    }
)
public class Flat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tower_id", nullable = false, foreignKey = @ForeignKey(name = "fk_flat_tower"))
    private Tower tower;

    @Column(name = "flat_number", nullable = false, length = 20)
    private String flatNumber; // e.g. "1203"

    @Column(name = "floor_number", nullable = false)
    private int floorNumber;

    @Column(name = "area_sq_ft", nullable = false)
    private double areaSqFt;

    @Column(name = "occupancy_status", length = 30)
    private String occupancyStatus = "OWNER_OCCUPIED";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Flat() {}

    public Flat(Tower tower, String flatNumber, int floorNumber, double areaSqFt, String occupancyStatus) {
        this.tower = tower;
        this.flatNumber = flatNumber;
        this.floorNumber = floorNumber;
        this.areaSqFt = areaSqFt;
        this.occupancyStatus = occupancyStatus;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Tower getTower() {
        return tower;
    }

    public void setTower(Tower tower) {
        this.tower = tower;
    }

    public String getFlatNumber() {
        return flatNumber;
    }

    public void setFlatNumber(String flatNumber) {
        this.flatNumber = flatNumber;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public void setFloorNumber(int floorNumber) {
        this.floorNumber = floorNumber;
    }

    public double getAreaSqFt() {
        return areaSqFt;
    }

    public void setAreaSqFt(double areaSqFt) {
        this.areaSqFt = areaSqFt;
    }

    public String getOccupancyStatus() {
        return occupancyStatus;
    }

    public void setOccupancyStatus(String occupancyStatus) {
        this.occupancyStatus = occupancyStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
