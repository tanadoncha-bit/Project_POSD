package com.example.itborrow.dto;
import com.example.itborrow.domain.entity.Equipment;
import com.example.itborrow.domain.enums.EquipmentStatus;
import java.math.BigDecimal;
public record EquipmentData(Long id,String assetCode,String name,EquipmentStatus status,Long categoryId,BigDecimal purchasePrice,String imageUrl,String specifications,String storageSlot) {
 public static EquipmentData from(Equipment e) { return new EquipmentData(e.getId(),e.getAssetCode(),e.getName(),e.getStatus(),e.getCategoryId(),e.getPurchasePrice(),e.getImageUrl(),e.getSpecifications(),e.getStorageSlot()); }
 public Equipment toEntity() { var e=new Equipment();e.setId(id);e.setAssetCode(assetCode);e.setName(name);e.setStatus(status);e.setCategoryId(categoryId);e.setPurchasePrice(purchasePrice);e.setImageUrl(imageUrl);e.setSpecifications(specifications);e.setStorageSlot(storageSlot);return e; }
}
