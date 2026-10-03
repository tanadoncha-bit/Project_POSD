package com.example.itborrow.controller.api;
import com.example.itborrow.service.WorkflowOperations;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;
@RestController
@RequestMapping("/api/v1")
public class WorkflowOperationsController {
 private final WorkflowOperations operations;
 public WorkflowOperationsController(WorkflowOperations operations) { this.operations=operations; }
 public record Action(String reason, String reference, BigDecimal amount) {}
 @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) @PostMapping("/borrow-requests/{id}/reject") public void reject(@PathVariable Long id,@RequestBody Action action) { operations.reject(id,action.reason()); }
 @GetMapping("/borrow-requests/{id}/settlement") public Map<String,Object> settlement(@PathVariable Long id) { return operations.settlement(id); }
 @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) @PostMapping("/borrow-requests/{id}/settlement") public void settle(@PathVariable Long id,@RequestBody Action action) { operations.settle(id,action.reference(),action.amount()); }
 @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) @PostMapping("/equipment/{id}/repair") public void repair(@PathVariable Long id,@RequestBody Action action) { operations.repair(id,action.reason()); }
}
