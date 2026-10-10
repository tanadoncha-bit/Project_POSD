package com.example.itborrow.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BorrowItemResponseDto {

    private LocalDate returnedOn;

    public LocalDate getReturnedOn() {
        return returnedOn;
    }

    public void setReturnedOn(LocalDate value) {
        returnedOn = value;
    }

    private BigDecimal purchasePrice;

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal value) {
        purchasePrice = value;
    }

    private Long equipmentId;
    private String assetCode;
    private String equipmentName;
    private String categoryName;

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String value) {
        categoryName = value;
    }

    private String imageUrl;

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    private String storageSlot;

    public String getStorageSlot() {
        return storageSlot;
    }

    public void setStorageSlot(String value) {
        storageSlot = value;
    }

    private int quantity;

    public BorrowItemResponseDto() {}

    public BorrowItemResponseDto(
            Long equipmentId, String assetCode, String equipmentName, int quantity) {
        this.equipmentId = equipmentId;
        this.assetCode = assetCode;
        this.equipmentName = equipmentName;
        this.quantity = quantity;
    }

    public Long getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId) {
        this.equipmentId = equipmentId;
    }

    public String getAssetCode() {
        return assetCode;
    }

    public void setAssetCode(String assetCode) {
        this.assetCode = assetCode;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName) {
        this.equipmentName = equipmentName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
