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
                .items(itemDtos)
                .build();
    }

    private BorrowItemResponseDto toItemResponseDto(BorrowItem item) {
        return new BorrowItemResponseDto(
                item.getEquipment().getId(),
                item.getEquipment().getAssetCode(),
                item.getEquipment().getName(),
                item.getQuantity()
        );
    }
}