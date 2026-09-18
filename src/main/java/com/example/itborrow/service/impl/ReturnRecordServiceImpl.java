package com.example.itborrow.service.impl;

import com.example.itborrow.domain.entity.BorrowItem;
import com.example.itborrow.domain.entity.BorrowRequest;
import com.example.itborrow.domain.entity.Equipment;
import com.example.itborrow.domain.entity.ReturnRecord;
import com.example.itborrow.domain.enums.EquipmentStatus;
import com.example.itborrow.domain.state.BorrowStateResolver;
import com.example.itborrow.dto.request.ReturnRequestDto;
import com.example.itborrow.dto.response.ReturnResponseDto;
import com.example.itborrow.exception.ResourceNotFoundException;
import com.example.itborrow.repository.BorrowRequestRepository;
import com.example.itborrow.repository.EquipmentRepository;
import com.example.itborrow.repository.ReturnRecordRepository;
import com.example.itborrow.service.FineStrategyService;
import com.example.itborrow.service.ReturnRecordService;
import com.example.itborrow.service.impl.strategy.FineStrategyResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ReturnRecordServiceImpl implements ReturnRecordService {

    private final ReturnRecordRepository returnRecordRepository;
    private final BorrowRequestRepository borrowRequestRepository;
    private final EquipmentRepository equipmentRepository;
    private final BorrowStateResolver stateResolver;
    private final FineStrategyResolver fineStrategyResolver;

    public ReturnRecordServiceImpl(ReturnRecordRepository returnRecordRepository,
                                    BorrowRequestRepository borrowRequestRepository,
                                    EquipmentRepository equipmentRepository,
                                    BorrowStateResolver stateResolver,
                                    FineStrategyResolver fineStrategyResolver) {
        this.returnRecordRepository = returnRecordRepository;
        this.borrowRequestRepository = borrowRequestRepository;
        this.equipmentRepository = equipmentRepository;
        this.stateResolver = stateResolver;
        this.fineStrategyResolver = fineStrategyResolver;
    }

    @Override
    @Transactional
    public ReturnResponseDto returnEquipment(Long borrowRequestId, ReturnRequestDto dto) {
        BorrowRequest request = borrowRequestRepository.findById(borrowRequestId)
                .orElseThrow(() -> ResourceNotFoundException.of("BorrowRequest", borrowRequestId));

        // 1) เปลี่ยนสถานะผ่าน State Pattern (BORROWED/OVERDUE -> RETURNED)
        //    ถ้าสถานะปัจจุบันคืนไม่ได้ (เช่น ยังเป็น PENDING) ตัวนี้จะ throw InvalidBorrowStateException ให้เอง
        stateResolver.resolve(request.getStatus()).returnEquipment(request);

        // 2) คำนวณค่าปรับผ่าน Strategy Pattern ตาม Role ของผู้ยืม
        FineStrategyService strategy = fineStrategyResolver.resolve(request.getUser().getRole());
        BigDecimal fine = strategy.calculate(request, dto.getReturnDate());

        // 3) คืนอุปกรณ์ทุกชิ้นกลับสถานะ AVAILABLE
        for (BorrowItem item : request.getItems()) {
            Equipment equipment = item.getEquipment();
            equipment.setStatus(EquipmentStatus.AVAILABLE);
            equipmentRepository.save(equipment);
        }

        // 4) บันทึก ReturnRecord
        ReturnRecord record = new ReturnRecord();
        record.setBorrowRequest(request);
        record.setReturnDate(dto.getReturnDate());
        record.setCondition(dto.getCondition());
        record.setFineAmount(fine);
        record.setRemark(dto.getRemark());

        borrowRequestRepository.save(request);
        ReturnRecord saved = returnRecordRepository.save(record);

        return toResponseDto(saved);
    }

    @Override
    public ReturnResponseDto getByBorrowRequestId(Long borrowRequestId) {
        ReturnRecord record = returnRecordRepository.findByBorrowRequestId(borrowRequestId)
                .orElseThrow(() -> ResourceNotFoundException.of("ReturnRecord (by borrowRequestId)", borrowRequestId));
        return toResponseDto(record);
    }

    private ReturnResponseDto toResponseDto(ReturnRecord record) {
        return ReturnResponseDto.builder()
                .id(record.getId())
                .borrowRequestId(record.getBorrowRequest().getId())
                .returnDate(record.getReturnDate())
                .condition(record.getCondition())
                .fineAmount(record.getFineAmount())
                .remark(record.getRemark())
                .build();
    }
}