package com.example.itborrow.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class BorrowItemRequestDto {

    @NotNull(message = "equipmentId ห้ามว่าง")
    private Long equipmentId;

    @Min(value = 1, message = "quantity ต้องมากกว่า 0")
    private int quantity = 1;

    public BorrowItemRequestDto() {
    }

    public BorrowItemRequestDto(Long equipmentId, int quantity) {
        this.equipmentId = equipmentId;
        this.quantity = quantity;
    }

    public Long getEquipmentId() {
        return equipmentId;
    }
    public void setEquipmentId(Long equipmentId) {
        this.equipmentId = equipmentId;
    }

    public int getQuantity() {
        return quantity;
    }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}