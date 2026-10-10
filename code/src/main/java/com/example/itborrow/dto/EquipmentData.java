package com.example.itborrow.dto;

import com.example.itborrow.domain.entity.Equipment;
import com.example.itborrow.domain.enums.EquipmentStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record EquipmentData(
        Long id,
        @Size(max = 30) String assetCode,
        @NotBlank @Size(max = 150) String name,
        EquipmentStatus status,
        @Positive Long categoryId,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal purchasePrice,
        @Size(max = 1000) String imageUrl,
        @Size(max = 10000) String specifications,
        @Size(max = 100) String storageSlot,
        @JsonProperty(access = JsonProperty.Access.READ_ONLY) Boolean reserved) {
    public static EquipmentData from(Equipment e) {
        return new EquipmentData(
                e.getId(),
                e.getAssetCode(),
                e.getName(),
                e.getStatus(),
                e.getCategoryId(),
                e.getPurchasePrice(),
                e.getImageUrl(),
                e.getSpecifications(),
                e.getStorageSlot(),
                e.isReserved());
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
