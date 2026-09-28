package com.example.itborrow.controller.web;
import com.example.itborrow.service.*;
import com.example.itborrow.repository.BorrowRequestRepository;
import com.example.itborrow.mapper.BorrowRequestMapper;
import com.example.itborrow.domain.enums.EquipmentStatus;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
@Controller
public class DashboardController {
    private final EquipmentService equipment;
    private final CurrentUser current;
    private final BorrowRequestRepository requests;
    private final BorrowRequestMapper mapper;
    public DashboardController(EquipmentService equipment, CurrentUser current, BorrowRequestRepository requests, BorrowRequestMapper mapper) {
        this.equipment=equipment; this.current=current; this.requests=requests; this.mapper=mapper;
    }
    @GetMapping({"/", "/Dashboard"}) public String home(Model model) {
        model.addAttribute("equipments",equipment.getAllEquipments(PageRequest.of(0,4)).getContent()); return "dashboard/index";
    }
    @GetMapping("/borrow") public String borrow(@RequestParam(required=false) Long equipmentId, @RequestParam(defaultValue="false") boolean modal, Model model) {
        model.addAttribute("selectedEquipmentId",equipmentId);
        model.addAttribute("availableEquipments",equipment.getAllEquipments(Pageable.unpaged()).getContent().stream().filter(e -> e.getStatus()==EquipmentStatus.AVAILABLE).toList());
        return modal ? "borrow/form :: borrowForm" : "borrow/form";
    }
    @GetMapping("/my-requests") @Transactional(readOnly=true) public String history(Model model) {
        var loans=requests.findByUserId(current.require().getId(),Pageable.unpaged(Sort.by("id").descending())).map(mapper::toResponseDto).getContent();
        model.addAttribute("requests",loans);
        model.addAttribute("overdueCount",loans.stream().filter(b -> "OVERDUE".equals(b.getStatus())).count());
        return "borrow/my-history";
    }
    @GetMapping("/profile") @Transactional(readOnly=true) public String profile(Model model) {
        var loans=requests.findByUserId(current.require().getId(),Pageable.unpaged(Sort.by("id").descending())).map(mapper::toResponseDto).getContent();
        model.addAttribute("activeLoans",loans.stream().filter(b -> java.util.Set.of("BORROWED","OVERDUE").contains(b.getStatus())).toList());
        model.addAttribute("borrowHistory",loans.stream().filter(b -> java.util.Set.of("RETURNED","CANCELLED").contains(b.getStatus())).toList());
        return "profile/index";
    }
    @GetMapping("/admin") public String admin() { return "admin/index"; }
}
