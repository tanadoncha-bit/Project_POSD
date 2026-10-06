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

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    @Column(name = "purchase_price", precision = 12, scale = 2)
    private java.math.BigDecimal purchasePrice;

    public java.math.BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(java.math.BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    @Column(name = "image_url", length = 1000)
    private String imageUrl;
    @Column(name = "specifications", columnDefinition = "TEXT")
    private String specifications;

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String value) {
        imageUrl = value;
    }

    public String getSpecifications() {
        return specifications;
    }

    public void setSpecifications(String value) {
        specifications = value;
    }

    @Column(name = "storage_slot", length = 100)
    private String storageSlot;

    public String getStorageSlot() {
        return storageSlot;
    }

    public void setStorageSlot(String value) {
        storageSlot = value;
    }

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