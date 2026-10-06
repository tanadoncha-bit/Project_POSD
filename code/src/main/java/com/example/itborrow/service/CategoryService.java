package com.example.itborrow.service;

import com.example.itborrow.dto.request.CategoryRequestDto;
import com.example.itborrow.dto.response.CategoryResponseDto;
import java.util.List;
import java.util.Map;

public interface CategoryService {
    List<CategoryResponseDto> list();

    CategoryResponseDto get(Long id);

    CategoryResponseDto create(CategoryRequestDto dto);

    CategoryResponseDto update(Long id, CategoryRequestDto dto);

    void delete(Long id);

    Map<Long, String> names();
}
