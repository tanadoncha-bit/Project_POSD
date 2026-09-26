package com.example.itborrow.domain.entity;

import com.example.itborrow.domain.enums.EquipmentStatus;

import jakarta.persistence.*;

@Entity
@Table(name = "equipment")
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_code", nullable = false, unique = true)
    private String assetCode;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentStatus status;

    public Equipment() {
    }

    // Constructor
    public Equipment(Long id, String assetCode, String name, EquipmentStatus status) {
        this.id = id;
        this.assetCode = assetCode;
        this.name = name;
        this.status = status;
    }

    // Constructor
    public Equipment(String assetCode, String name, EquipmentStatus status) {
        this.assetCode = assetCode;
        this.name = name;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAssetCode() {
        return assetCode;
    }

    public void setAssetCode(String assetCode) {
        this.assetCode = assetCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EquipmentStatus getStatus() {
        return status;
    }

    public void setStatus(EquipmentStatus status) {
        this.status = status;
    }
}