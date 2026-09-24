package com.example.itborrow.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.example.itborrow.domain.entity.Equipment;
import com.example.itborrow.repository.EquipmentRepository;
import com.example.itborrow.service.EquipmentService;


@Service
public class EquipmentServiceImpl implements EquipmentService {

    private final EquipmentRepository equipmentRepository;

    // ทำ Dependency Injection ผ่าน Constructor
    public EquipmentServiceImpl(EquipmentRepository equipmentRepository) {
        this.equipmentRepository = equipmentRepository;
    }

    @Override
    public Page<Equipment> getAllEquipments(Pageable pageable) {
    return equipmentRepository.findAll(pageable);
    }

    @Override
    public Equipment getEquipmentById(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Equipment not found with id: " + id));
    }

    @Override
    public Equipment createEquipment(Equipment equipment) {
        return equipmentRepository.save(equipment);
    }

    @Override
    public Equipment updateEquipment(Long id, Equipment equipmentDetails) {
        Equipment equipment = getEquipmentById(id);
        equipment.setName(equipmentDetails.getName());
        equipment.setStatus(equipmentDetails.getStatus());
        // อัปเดตฟิลด์อื่นๆ ตามต้องการ
        return equipmentRepository.save(equipment);
    }

    @Override
    public void deleteEquipment(Long id) {
        equipmentRepository.deleteById(id);
    }
}
