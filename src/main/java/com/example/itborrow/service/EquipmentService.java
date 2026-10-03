package com.example.itborrow.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.example.itborrow.domain.entity.Equipment;

public interface EquipmentService {
    Page<Equipment> searchInventory(String keyword, com.example.itborrow.domain.enums.EquipmentStatus status, Pageable pageable);
    java.util.Map<String,Long> inventorySummary();
    Page<Equipment> getAllEquipments(Pageable pageable);
    Page<Equipment> searchEquipments(String keyword, Pageable pageable);
    Equipment getEquipmentById(Long id);
    Equipment createEquipment(Equipment equipment);
    Equipment updateEquipment(Long id, Equipment equipmentDetails);
    void deleteEquipment(Long id);
}
