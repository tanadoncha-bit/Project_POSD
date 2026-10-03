package com.example.itborrow.dto.response;

public class BorrowItemResponseDto {

    private java.time.LocalDate returnedOn;
    public java.time.LocalDate getReturnedOn() { return returnedOn; }
    public void setReturnedOn(java.time.LocalDate value) { returnedOn=value; }
    private java.math.BigDecimal purchasePrice;
    public java.math.BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(java.math.BigDecimal value) { purchasePrice=value; }
    private Long equipmentId;
    private String assetCode;
    private String equipmentName;
    private String categoryName;
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String value) { categoryName = value; }
    private String imageUrl;
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    private String storageSlot;
    public String getStorageSlot() { return storageSlot; }
    public void setStorageSlot(String value) { storageSlot = value; }
    private int quantity;

    public BorrowItemResponseDto() {
    }

    public BorrowItemResponseDto(Long equipmentId, String assetCode, String equipmentName, int quantity) {
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