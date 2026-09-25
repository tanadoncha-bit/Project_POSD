package com.example.itborrow.controller.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.itborrow.service.EquipmentService;

@Controller
public class DashboardController {

    private final EquipmentService equipmentService;

    public DashboardController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping({"/", "/Dashboard"})
    public String showDashboard(Model model) {
        model.addAttribute("equipments", equipmentService.getAllEquipments(PageRequest.of(0, 4)).getContent());

        return "dashboard/index";
    }
}