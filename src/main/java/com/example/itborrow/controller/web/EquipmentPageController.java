package com.example.itborrow.controller.web;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.itborrow.domain.entity.Equipment;
import com.example.itborrow.service.EquipmentService;

@Controller
@RequestMapping("/equipment")
public class EquipmentPageController {

    private final EquipmentService equipmentService;

    public EquipmentPageController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping
    public String showEquipmentList(Model model) {
        List<Equipment> equipments = equipmentService.getAllEquipments();
        model.addAttribute("equipments", equipments);

        return "equipment/list";
    }

    @GetMapping("/{id}")
    public String showEquipmentDetail(@PathVariable Long id, Model model) {
        Equipment equipment = equipmentService.getEquipmentById(id);
        model.addAttribute("equipment", equipment);

        return "equipment/detail";
    }
}