package com.example.itborrow.controller.api;

import com.example.itborrow.domain.enums.EquipmentStatus;
import com.example.itborrow.dto.EquipmentData;
import com.example.itborrow.dto.response.PageResponse;
import com.example.itborrow.service.EquipmentService;

import jakarta.validation.Valid;

import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/equipment")
public class EquipmentController {
    private final EquipmentService service;

    public EquipmentController(EquipmentService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<EquipmentData> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) EquipmentStatus status,
            Pageable pageable) {
        var page =
                PageRequest.of(
                        pageable.getPageNumber(),
                        Math.min(100, pageable.getPageSize()),
                        pageable.getSort());
        return PageResponse.from(
                service.searchInventory(keyword, status, page).map(EquipmentData::from));
    }

    @GetMapping("/summary")
    public Map<String, Long> summary() {
        return service.inventorySummary();
    }

    @GetMapping("/{id}")
    public EquipmentData get(@PathVariable Long id) {
        return EquipmentData.from(service.getEquipmentById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EquipmentData create(@Valid @RequestBody EquipmentData data) {
        return EquipmentData.from(service.createEquipment(data.toEntity()));
    }

    @PutMapping("/{id}")
    public EquipmentData update(@PathVariable Long id, @Valid @RequestBody EquipmentData data) {
        return EquipmentData.from(service.updateEquipment(id, data.toEntity()));
    }

    @GetMapping("/{id}/deletion")
    public Map<String, Boolean> deletion(@PathVariable Long id) {
        return Map.of("allowed", service.canDeleteEquipment(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.deleteEquipment(id);
    }
}
