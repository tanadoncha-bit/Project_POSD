package com.example.itborrow.mapper;

import com.example.itborrow.domain.entity.BorrowItem;
import com.example.itborrow.domain.entity.BorrowRequest;
import com.example.itborrow.dto.response.BorrowItemResponseDto;
import com.example.itborrow.dto.response.BorrowResponseDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BorrowRequestMapper {

    public BorrowResponseDto toResponseDto(BorrowRequest entity) {
        List<BorrowItemResponseDto> itemDtos = entity.getItems().stream()
                .map(this::toItemResponseDto)
                .toList();

        return BorrowResponseDto.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .username(entity.getUser().getUsername())
                .borrowDate(entity.getBorrowDate())
                .dueDate(entity.getDueDate())
                .status(entity.getStatus().name())
                .note(entity.getNote())
                .rejectionReason(entity.getRejectionReason())
                .feePolicy(new BorrowResponseDto.FeePolicy(entity.getDailyFine(),entity.getGraceDays(),entity.getScratchRate(),entity.getDamageRate(),entity.getLossRate()))
                .items(itemDtos)
                .build();
    }

    private BorrowItemResponseDto toItemResponseDto(BorrowItem item) {
        var result = new BorrowItemResponseDto(
                item.getEquipment().getId(),
                item.getSnapshotName() != null ? item.getSnapshotAssetCode() : item.getEquipment().getAssetCode(),
                item.getSnapshotName() != null ? item.getSnapshotName() : item.getEquipment().getName(),
                item.getQuantity()
        );
        result.setImageUrl(item.getSnapshotName() != null ? item.getSnapshotImageUrl() : item.getEquipment().getImageUrl());
        result.setReturnedOn(item.getReturnedOn());
        result.setPurchasePrice(item.getSnapshotPurchasePrice());
        result.setCategoryName(item.getSnapshotCategoryName());
        result.setStorageSlot(item.getSnapshotName() != null ? item.getSnapshotStorageSlot() : item.getEquipment().getStorageSlot());
        return result;
    }
}