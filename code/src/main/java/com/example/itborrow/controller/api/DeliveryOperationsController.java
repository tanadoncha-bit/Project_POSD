package com.example.itborrow.controller.api;

import com.example.itborrow.service.DeliveryOperations;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/delivery-jobs")
public class DeliveryOperationsController {
    private final DeliveryOperations service;
    public DeliveryOperationsController(DeliveryOperations service) { this.service = service; }
    @GetMapping("/summary") public Map<String, Object> summary() { return service.summary(); }
    @GetMapping("/failed") public List<Map<String, Object>> failed() { return service.failed(); }
    @PostMapping("/{id}/retry") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void retry(@PathVariable long id) { service.retry(id); }
}
