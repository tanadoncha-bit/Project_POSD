package com.example.itborrow.dto;

import com.example.itborrow.domain.entity.Equipment;
import com.example.itborrow.domain.enums.EquipmentStatus;
import java.math.BigDecimal;

public record EquipmentData(Long id, @jakarta.validation.constraints.Size(max=30) String assetCode, @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=150) String name, EquipmentStatus status, @jakarta.validation.constraints.Positive Long categoryId,
        @jakarta.validation.constraints.DecimalMin("0.00") @jakarta.validation.constraints.Digits(integer=10,fraction=2) BigDecimal purchasePrice, @jakarta.validation.constraints.Size(max=1000) String imageUrl, @jakarta.validation.constraints.Size(max=10000) String specifications, @jakarta.validation.constraints.Size(max=100) String storageSlot) {
    public static EquipmentData from(Equipment e) {
        return new EquipmentData(e.getId(), e.getAssetCode(), e.getName(), e.getStatus(), e.getCategoryId(),
                e.getPurchasePrice(), e.getImageUrl(), e.getSpecifications(), e.getStorageSlot());
    }

    public Equipment toEntity() {
        var e = new Equipment();
        e.setId(id);
        e.setAssetCode(assetCode);
        e.setName(name);
        e.setStatus(status);
        e.setCategoryId(categoryId);
        e.setPurchasePrice(purchasePrice);
        e.setImageUrl(imageUrl);
        e.setSpecifications(specifications);
        e.setStorageSlot(storageSlot);
        return e;
    }
}
