package com.example.itborrow.controller.api;
import com.example.itborrow.domain.entity.EquipmentCategory;
import com.example.itborrow.repository.EquipmentCategoryRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
public class CategoryController {
    private final EquipmentCategoryRepository categories;
    public CategoryController(EquipmentCategoryRepository categories) { this.categories=categories; }
    @GetMapping("/api/v1/categories") public List<EquipmentCategory> list() { return categories.findAll(); }
}
