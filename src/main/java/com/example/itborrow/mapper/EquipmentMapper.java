package com.example.itborrow.mapper;

import org.springframework.stereotype.Component;

import com.example.itborrow.domain.entity.Equipment;
import com.example.itborrow.domain.enums.EquipmentStatus;
import com.example.itborrow.dto.request.EquipmentRequestDto;
import com.example.itborrow.dto.response.EquipmentResponseDto;

@Component
public class EquipmentMapper {

    // แปลง Entity เป็น Response DTO
    public EquipmentResponseDto toDto(Equipment entity) {
        if (entity == null) {
            return null;
        }
        
        return new EquipmentResponseDto.Builder()
                .id(entity.getId())
                .name(entity.getName())
                // แปลง Enum เป็น String โดยใช้ .name()
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .build();
    }

    // แปลง Request DTO เป็น Entity
    public Equipment toEntity(EquipmentRequestDto dto) {
        if (dto == null) {
            return null;
        }
        
        Equipment equipment = new Equipment();
        equipment.setName(dto.getName());
        
        // แปลง String จาก DTO กลับเป็น Enum
        if (dto.getStatus() != null) {
            equipment.setStatus(EquipmentStatus.valueOf(dto.getStatus()));
        }
        
        return equipment;
    }
}