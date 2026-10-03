package com.example.itborrow.controller.web;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.itborrow.service.EquipmentService;

@Controller
@RequestMapping("/equipment")
public class EquipmentPageController {

    private final EquipmentService equipmentService;

    public EquipmentPageController(
            EquipmentService equipmentService
    ) {
        this.equipmentService = equipmentService;
    }

    @GetMapping
    public String showEquipmentList(@org.springframework.web.bind.annotation.RequestParam(defaultValue="0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue="") String keyword, Model model) {
        var results = equipmentService.searchEquipments(keyword, org.springframework.data.domain.PageRequest.of(Math.max(0,page),12,org.springframework.data.domain.Sort.by("name").and(org.springframework.data.domain.Sort.by("id"))));
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
