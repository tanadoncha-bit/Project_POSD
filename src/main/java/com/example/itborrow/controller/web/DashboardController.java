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
    private final com.example.itborrow.repository.EquipmentRepository assets;
    public DashboardController(EquipmentService equipment, CurrentUser current, BorrowRequestRepository requests, BorrowRequestMapper mapper, com.example.itborrow.repository.EquipmentRepository assets) {
        this.equipment=equipment; this.current=current; this.requests=requests; this.mapper=mapper; this.assets=assets;
    }
    @GetMapping({"/", "/Dashboard"}) public String home(Model model) {
        model.addAttribute("equipments",equipment.getAllEquipments(PageRequest.of(0,4)).getContent()); return "dashboard/index";
    }
    @GetMapping("/borrow") public String borrow(@RequestParam(required=false) Long equipmentId, @RequestParam(defaultValue="false") boolean modal, @RequestParam(required=false) java.util.List<Long> equipmentIds, Model model) {
        model.addAttribute("selectedEquipmentId",equipmentId);
        if (equipmentIds != null && equipmentIds.size() > 100) throw new IllegalArgumentException("Select at most 100 equipment items.");
        var selected = equipmentIds != null ? equipmentIds : equipmentId != null ? java.util.List.of(equipmentId) : null;
        model.addAttribute("availableEquipments", selected != null ? assets.findByIdInAndStatus(selected,EquipmentStatus.AVAILABLE) : assets.searchInventory("",EquipmentStatus.AVAILABLE,PageRequest.of(0,12,Sort.by("name","id"))).getContent());
        return modal ? "borrow/form :: borrowForm" : "borrow/form";
    }
    private java.util.List<com.example.itborrow.domain.enums.BorrowStatus> states(String status) {
        var all = com.example.itborrow.domain.enums.BorrowStatus.values();
        if ("ALL".equals(status)) return java.util.List.of(all);
        if ("ACTIVE".equals(status)) return java.util.List.of(com.example.itborrow.domain.enums.BorrowStatus.APPROVED, com.example.itborrow.domain.enums.BorrowStatus.BORROWED, com.example.itborrow.domain.enums.BorrowStatus.OVERDUE);
        try { return java.util.List.of(com.example.itborrow.domain.enums.BorrowStatus.valueOf(status)); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Unknown request status."); }
    }
    @GetMapping("/my-requests") @Transactional(readOnly=true)
    public String history(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="ALL") String status,
            @RequestParam(defaultValue="") String keyword, Model model) {
        var owner=((com.example.itborrow.domain.entity.User)model.getAttribute("currentUser")).getId();
        var results=requests.searchUserRequests(owner,states(status),keyword.trim(),PageRequest.of(Math.max(0,page),10,Sort.by("id").descending())).map(mapper::toResponseDto);
        model.addAttribute("requests",results.getContent()); model.addAttribute("requestPage",results);
        model.addAttribute("requestStatus",status); model.addAttribute("keyword",keyword);
        var counts=new java.util.HashMap<String,Long>();
        var statusCounts=new java.util.EnumMap<com.example.itborrow.domain.enums.BorrowStatus,Long>(com.example.itborrow.domain.enums.BorrowStatus.class);
        requests.summarizeUserStatuses(owner).forEach(row -> statusCounts.put(row.getStatus(),row.getTotal()));
        for(var filter:java.util.List.of("ALL","ACTIVE","PENDING","RETURNED","CANCELLED"))
            counts.put(filter,states(filter).stream().mapToLong(state -> statusCounts.getOrDefault(state,0L)).sum());
        model.addAttribute("requestCounts",counts);
        model.addAttribute("overdueCount",statusCounts.getOrDefault(com.example.itborrow.domain.enums.BorrowStatus.OVERDUE,0L));
        return "borrow/my-history";
    }
    @GetMapping("/profile") @Transactional(readOnly=true)
    public String profile(@RequestParam(defaultValue="0") int activePage, @RequestParam(defaultValue="0") int historyPage, Model model) {
        var owner=((com.example.itborrow.domain.entity.User)model.getAttribute("currentUser")).getId();
        var active=requests.searchUserRequests(owner,java.util.List.of(com.example.itborrow.domain.enums.BorrowStatus.BORROWED,com.example.itborrow.domain.enums.BorrowStatus.OVERDUE),"",PageRequest.of(Math.max(0,activePage),5,Sort.by("id").descending())).map(mapper::toResponseDto);
        var history=requests.searchUserRequests(owner,java.util.List.of(com.example.itborrow.domain.enums.BorrowStatus.RETURNED,com.example.itborrow.domain.enums.BorrowStatus.CANCELLED),"",PageRequest.of(Math.max(0,historyPage),5,Sort.by("id").descending())).map(mapper::toResponseDto);
        model.addAttribute("activeLoans",active.getContent()); model.addAttribute("borrowHistory",history.getContent());
        model.addAttribute("activeLoanPage",active); model.addAttribute("borrowHistoryPage",history);
        return "profile/index";
    }
    @GetMapping("/admin") public String admin() { return "admin/index"; }
}
