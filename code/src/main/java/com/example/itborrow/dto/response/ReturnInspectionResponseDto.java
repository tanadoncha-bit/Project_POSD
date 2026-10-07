package com.example.itborrow.dto.response;

import java.math.BigDecimal;
import com.example.itborrow.domain.entity.ReturnInspection;

public record ReturnInspectionResponseDto(Long equipmentId, String equipmentName, String condition,
        BigDecimal purchasePrice, BigDecimal damageRate, BigDecimal damageAmount, String remark) {
    public static ReturnInspectionResponseDto from(ReturnInspection value) {
        return new ReturnInspectionResponseDto(value.getEquipmentId(), value.getEquipmentName(), value.getCondition(),
                value.getPurchasePrice(), value.getDamageRate(), value.getDamageAmount(), value.getRemark());
    }
}
