package com.example.itborrow.controller.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.itborrow.domain.entity.Equipment;
import com.example.itborrow.service.EquipmentService;

@RestController
@RequestMapping("/api/v1/equipment")
public class EquipmentController {

    private final EquipmentService equipmentService;

    // ทำ Dependency Injection ผ่าน Constructor
    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    // Read: ดึงข้อมูลทั้งหมดแบบแบ่งหน้าและเรียงลำดับ (Pagination/Sorting)
    @GetMapping
    public ResponseEntity<Page<Equipment>> getAllEquipment(Pageable pageable) {
        // หมายเหตุ: ต้องไปเพิ่มเมธอด getAllEquipments(Pageable pageable) ใน EquipmentService ด้วย
        Page<Equipment> equipments = equipmentService.getAllEquipments(pageable);
        return ResponseEntity.ok(equipments);
    }

    // Read: ดึงข้อมูลตาม ID
    @GetMapping("/{id}")
    public ResponseEntity<Equipment> getEquipmentById(@PathVariable Long id) {
        Equipment equipment = equipmentService.getEquipmentById(id);
        return ResponseEntity.ok(equipment);
    }

    // Create: สร้างอุปกรณ์ใหม่
    @PostMapping
    public ResponseEntity<Equipment> createEquipment(@RequestBody Equipment equipment) {
        Equipment createdEquipment = equipmentService.createEquipment(equipment);
        return new ResponseEntity<>(createdEquipment, HttpStatus.CREATED);
    }

    // Update: แก้ไขข้อมูลอุปกรณ์
    @PutMapping("/{id}")
    public ResponseEntity<Equipment> updateEquipment(@PathVariable Long id, @RequestBody Equipment equipmentDetails) {
        Equipment updatedEquipment = equipmentService.updateEquipment(id, equipmentDetails);
        return ResponseEntity.ok(updatedEquipment);
    }

    // Delete: ลบอุปกรณ์
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEquipment(@PathVariable Long id) {
        equipmentService.deleteEquipment(id);
        return ResponseEntity.noContent().build();
    }
}