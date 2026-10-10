package com.example.itborrow.controller.web;

import com.example.itborrow.config.PaginationProperties;
import com.example.itborrow.service.EquipmentService;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/equipment")
public class EquipmentPageController {
    private final PaginationProperties pagination;

    private final EquipmentService equipmentService;

    public EquipmentPageController(
            EquipmentService equipmentService, PaginationProperties pagination) {
        this.pagination = pagination;
        this.equipmentService = equipmentService;
    }

    @GetMapping
    public String showEquipmentList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String keyword,
            Model model) {
        var results =
                equipmentService.searchEquipments(
                        keyword,
                        PageRequest.of(
                                Math.max(0, page),
                                pagination.getCatalogSize(),
                                Sort.by("name").and(Sort.by("id"))));
        model.addAttribute("equipments", results.getContent());
        model.addAttribute("equipmentPage", results);
        model.addAttribute("keyword", keyword);
        return "equipment/list";
    }

    @GetMapping("/{id}")
    public String showEquipmentDetail(@PathVariable Long id, Model model) {
        model.addAttribute("equipment", equipmentService.getEquipmentById(id));

        return "equipment/detail";
    }
}
