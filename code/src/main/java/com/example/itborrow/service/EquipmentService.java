package com.example.itborrow.service;

import com.example.itborrow.domain.entity.Equipment;
import com.example.itborrow.domain.enums.EquipmentStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface EquipmentService {
    Page<Equipment> searchInventory(String keyword, EquipmentStatus status, Pageable pageable);

    Map<String, Long> inventorySummary();

    Page<Equipment> getAllEquipments(Pageable pageable);

    Page<Equipment> searchEquipments(String keyword, Pageable pageable);

    Equipment getEquipmentById(Long id);

    Equipment createEquipment(Equipment equipment);

    Equipment updateEquipment(Long id, Equipment equipmentDetails);

    boolean canDeleteEquipment(Long id);

    void deleteEquipment(Long id);
}
