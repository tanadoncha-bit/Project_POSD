package com.example.itborrow.service.impl;

import com.example.itborrow.domain.entity.EquipmentCategory;
import com.example.itborrow.domain.enums.Role;
import com.example.itborrow.dto.request.CategoryRequestDto;
import com.example.itborrow.dto.response.CategoryResponseDto;
import com.example.itborrow.exception.*;
import com.example.itborrow.mapper.CategoryMapper;
import com.example.itborrow.repository.*;
import com.example.itborrow.service.*;

import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    private final EquipmentCategoryRepository categories;
    private final EquipmentRepository equipment;
    private final CategoryMapper mapper;
    private final CurrentUser current;

    public CategoryServiceImpl(
            EquipmentCategoryRepository categories,
            EquipmentRepository equipment,
            CategoryMapper mapper,
            CurrentUser current) {
        this.categories = categories;
        this.equipment = equipment;
        this.mapper = mapper;
        this.current = current;
    }

    private void requireAdmin() {
        if (current.require().getRole() != Role.ADMIN)
            throw new AccessDeniedException("Administrator access required.");
    }

    private EquipmentCategory entity(Long id) {
        return categories
                .findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id));
    }

    public List<CategoryResponseDto> list() {
        return categories.findAll(Sort.by("name", "id")).stream().map(mapper::toResponse).toList();
    }

    public CategoryResponseDto get(Long id) {
        return mapper.toResponse(entity(id));
    }

    @Transactional
    public CategoryResponseDto create(CategoryRequestDto dto) {
        requireAdmin();
        return mapper.toResponse(
                categories.save(new EquipmentCategory(dto.name().trim(), dto.description())));
    }

    @Transactional
    public CategoryResponseDto update(Long id, CategoryRequestDto dto) {
        requireAdmin();
        var category = entity(id);
        category.setName(dto.name().trim());
        category.setDescription(dto.description());
        return mapper.toResponse(categories.save(category));
    }

    @Transactional
    public void delete(Long id) {
        requireAdmin();
        var category = entity(id);
        if (equipment.existsByCategoryId(id))
            throw new InvalidBorrowStateException(
                    "Move equipment out of this category before deleting it.");
        categories.delete(category);
        categories.flush();
    }

    public Map<Long, String> names() {
        return list().stream()
                .collect(Collectors.toMap(CategoryResponseDto::id, CategoryResponseDto::name));
    }
}
