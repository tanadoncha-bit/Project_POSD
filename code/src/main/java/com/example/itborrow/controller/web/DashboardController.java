package com.example.itborrow.controller.web;

import com.example.itborrow.service.BorrowerPageQuery;
import com.example.itborrow.service.EquipmentService;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class DashboardController {
    private final EquipmentService equipment;
    private final BorrowerPageQuery pages;

    public DashboardController(EquipmentService equipment, BorrowerPageQuery pages) {
        this.equipment = equipment;
        this.pages = pages;
    }

    @GetMapping({"/", "/Dashboard"})
    public String home(Model model) {
        model.addAttribute(
                "equipments", equipment.getAllEquipments(PageRequest.of(0, 4)).getContent());
        return "dashboard/index";
    }

    @GetMapping("/borrow")
    public String borrow(
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(defaultValue = "false") boolean modal,
            @RequestParam(required = false) List<Long> equipmentIds,
            Model model) {
        model.addAttribute("selectedEquipmentId", equipmentId);
        model.addAttribute(
                "availableEquipments", pages.availableEquipment(equipmentId, equipmentIds));
        return modal ? "borrow/form :: borrowForm" : "borrow/form";
    }

    @GetMapping("/my-requests")
    public String history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "") String keyword,
            Model model) {
        model.addAllAttributes(pages.history(page, status, keyword));
        return "borrow/my-history";
    }

    @GetMapping("/profile")
    public String profile(
            @RequestParam(defaultValue = "0") int activePage,
            @RequestParam(defaultValue = "0") int historyPage,
            Model model) {
        model.addAllAttributes(pages.profile(activePage, historyPage));
        return "profile/index";
    }

    @GetMapping("/admin")
    public String admin() {
        return "admin/index";
    }
}
