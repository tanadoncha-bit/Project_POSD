package com.example.itborrow.controller.api;

import com.example.itborrow.service.*;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class WorkflowOperationsController {
    private final RequestRejectionService rejections;
    private final SettlementService settlements;
    private final EquipmentRepairService repairs;

    public WorkflowOperationsController(RequestRejectionService rejections, SettlementService settlements,
            EquipmentRepairService repairs) {
        this.rejections = rejections;
        this.settlements = settlements;
        this.repairs = repairs;
    }

    public record Action(String reason, String reference, BigDecimal amount) {
    }

    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    @PostMapping("/borrow-requests/{id}/reject")
    public void reject(@PathVariable Long id, @RequestBody Action action) {
        rejections.reject(id, action.reason());
    }

    @GetMapping("/borrow-requests/{id}/settlement")
    public Map<String, Object> settlement(@PathVariable Long id) {
        return settlements.settlement(id);
    }

    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    @PostMapping("/borrow-requests/{id}/settlement")
    public void settle(@PathVariable Long id, @RequestBody Action action) {
        settlements.settle(id, action.reference(), action.amount());
    }

    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    @PostMapping("/equipment/{id}/repair")
    public void repair(@PathVariable Long id, @RequestBody Action action) {
        repairs.repair(id, action.reason());
    }
}
