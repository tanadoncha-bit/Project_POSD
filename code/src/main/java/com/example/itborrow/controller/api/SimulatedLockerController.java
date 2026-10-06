package com.example.itborrow.controller.api;

import com.example.itborrow.service.SimulatedLockerService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/borrow-requests/{id}/locker")
public class SimulatedLockerController {
    private final SimulatedLockerService service;

    public SimulatedLockerController(SimulatedLockerService service) {
        this.service = service;
    }

    public record OpenRequest(String pin) {
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> access(@PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(service.access(id));
    }

    @PostMapping
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void open(@PathVariable Long id, @RequestBody OpenRequest request) {
        service.open(id, request.pin());
    }
}
