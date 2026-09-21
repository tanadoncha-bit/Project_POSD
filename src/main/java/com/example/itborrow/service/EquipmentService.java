package com.example.itborrow.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.example.itborrow.domain.entity.Equipment;

public interface EquipmentService {
    Page<Equipment> getAllEquipments(Pageable pageable);
    Equipment getEquipmentById(Long id);
    Equipment createEquipment(Equipment equipment);
    Equipment updateEquipment(Long id, Equipment equipmentDetails);
    void deleteEquipment(Long id);
}
