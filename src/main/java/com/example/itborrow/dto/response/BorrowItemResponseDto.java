package com.example.itborrow.dto.response;

public class BorrowItemResponseDto {

    private Long equipmentId;
    private String assetCode;
    private String equipmentName;
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