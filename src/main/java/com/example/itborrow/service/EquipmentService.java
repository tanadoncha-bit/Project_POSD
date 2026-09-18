package com.example.itborrow.service;

import java.util.List;

import com.example.itborrow.domain.entity.Equipment;

public interface EquipmentService {
    List<Equipment> getAllEquipments();
    Equipment getEquipmentById(Long id);
    Equipment createEquipment(Equipment equipment);
    Equipment updateEquipment(Long id, Equipment equipmentDetails);
    void deleteEquipment(Long id);
}
