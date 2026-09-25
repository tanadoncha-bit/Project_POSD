package com.example.itborrow.controller.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.itborrow.service.EquipmentService;

@Controller
public class DashboardController {

    private final EquipmentService equipmentService;

    public DashboardController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping({"/", "/Dashboard"})
    public String showDashboard(Model model) {
        model.addAttribute(
                "equipments",
                equipmentService.getAllEquipments(PageRequest.of(0, 4)).getContent()
        );

        return "dashboard/index";
    }

    @GetMapping("/borrow")
    public String showBorrowForm(
            @RequestParam(required = false) Long equipmentId,
            Model model
    ) {
        model.addAttribute("selectedEquipmentId", equipmentId);
        model.addAttribute(
                "equipments",
                equipmentService.getAllEquipments(Pageable.unpaged()).getContent()
        );

        return "borrow/form";
    }

    @GetMapping("/my-requests")
    public String showMyRequests() {
        return "borrow/my-history";
    }

    @GetMapping("/profile")
    public String showProfile() {
        return "profile/index";
    }

    @GetMapping("/admin")
    public String showAdminDashboard() {
        return "admin/index";
    }
}
