package com.example.itborrow.service.impl;
import com.example.itborrow.repository.*;
import com.example.itborrow.domain.enums.*;
import com.example.itborrow.exception.*;
import com.example.itborrow.common.event.BorrowWorkflowEvent;
import com.example.itborrow.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import java.math.BigDecimal;
import java.util.Map;
@Service
public class EquipmentRepairServiceImpl implements EquipmentRepairService {
    private final EquipmentRepository equipment;
    private final CurrentUser current;
    private final EquipmentRepairRepository repairs;
    public EquipmentRepairServiceImpl(EquipmentRepository equipment, CurrentUser current, EquipmentRepairRepository repairs) {
        this.equipment=equipment;
        this.current=current;
        this.repairs=repairs;
    }
    private String required(String value) {
        if (value == null || value.isBlank() || value.length() > 500)
            throw new IllegalArgumentException("Provide a reason/reference of at most 500 characters.");
        return value.trim();
    }

    @Transactional
    public void repair(Long id, String note) {
        current.requireOperator();
        var asset = equipment.findLockedById(id).orElseThrow(() -> ResourceNotFoundException.of("Equipment", id));
        if (asset.getStatus() != EquipmentStatus.MAINTENANCE)
            throw new InvalidBorrowStateException("Only equipment under maintenance can be repaired.");
        repairs.record(id,current.require().getUsername(),required(note));
        asset.setStatus(EquipmentStatus.AVAILABLE);
    }
}
