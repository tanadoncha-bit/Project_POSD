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
    public String showEquipmentList(Model model) {
        model.addAttribute(
                "equipments",
                equipmentService.getAllEquipments(Pageable.unpaged()).getContent()
        );

        return "equipment/list";
    }

    @GetMapping("/{id}")
    public String showEquipmentDetail(@PathVariable Long id, Model model) {
        model.addAttribute("equipment", equipmentService.getEquipmentById(id));

        return "equipment/detail";
    }
}
