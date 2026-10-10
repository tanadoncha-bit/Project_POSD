package com.example.itborrow.mapper;

import com.example.itborrow.domain.entity.EquipmentCategory;
import com.example.itborrow.dto.response.CategoryResponseDto;

import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {
    public CategoryResponseDto toResponse(EquipmentCategory category) {
        return new CategoryResponseDto(
                category.getId(), category.getName(), category.getDescription());
    }
}
