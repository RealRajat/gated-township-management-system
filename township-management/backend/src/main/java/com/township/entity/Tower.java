package com.township.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "towers")
public class Tower {

    @Id
    @Column(length = 10, nullable = false)
    private String id; // e.g. "T01"

    @Column(nullable = false, length = 100)
    private String name; // e.g. "Tower A (Orchid)"

    @Column(nullable = false)
    private int floors = 15;

    @Column(nullable = false)
    private int totalUnits = 86;

    @Column(length = 50)
    private String type = "Residential";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Tower() {}

    public Tower(String id, String name, int floors, int totalUnits, String type) {
        this.id = id;
        this.name = name;
        this.floors = floors;
        this.totalUnits = totalUnits;
        this.type = type;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getFloors() {
        return floors;
    }

    public void setFloors(int floors) {
        this.floors = floors;
    }

    public int getTotalUnits() {
        return totalUnits;
    }

    public void setTotalUnits(int totalUnits) {
        this.totalUnits = totalUnits;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
