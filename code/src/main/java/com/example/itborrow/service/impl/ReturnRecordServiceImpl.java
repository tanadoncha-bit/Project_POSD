package com.example.itborrow.service.impl;

import com.example.itborrow.common.event.BorrowWorkflowEvent;
import com.example.itborrow.config.FeePolicyProperties;
import com.example.itborrow.domain.entity.BorrowItem;
import com.example.itborrow.domain.entity.BorrowRequest;
import com.example.itborrow.domain.entity.Equipment;
import com.example.itborrow.domain.entity.ReturnInspection;
import com.example.itborrow.domain.entity.ReturnRecord;
import com.example.itborrow.domain.enums.BorrowStatus;
import com.example.itborrow.domain.enums.EquipmentStatus;
import com.example.itborrow.domain.enums.ReturnCondition;
import com.example.itborrow.domain.state.BorrowStateResolver;
import com.example.itborrow.dto.request.ReturnRequestDto;
import com.example.itborrow.dto.response.ReturnInspectionResponseDto;
import com.example.itborrow.dto.response.ReturnResponseDto;
import com.example.itborrow.exception.InvalidBorrowStateException;
import com.example.itborrow.exception.ResourceNotFoundException;
import com.example.itborrow.repository.BorrowRequestRepository;
import com.example.itborrow.repository.EquipmentRepository;
import com.example.itborrow.repository.ReturnRecordRepository;
import com.example.itborrow.service.CurrentUser;
import com.example.itborrow.service.FineStrategyService;
import com.example.itborrow.service.ReturnRecordService;
import com.example.itborrow.service.strategy.FineStrategyResolver;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
public class ReturnRecordServiceImpl implements ReturnRecordService {
    private final Clock clock;
    private final FeePolicyProperties fees;

    private final ApplicationEventPublisher events;
    private final CurrentUser current;
    private final ReturnRecordRepository returnRecordRepository;
    private final BorrowRequestRepository borrowRequestRepository;
    private final EquipmentRepository equipmentRepository;
    private final BorrowStateResolver stateResolver;
    private final FineStrategyResolver fineStrategyResolver;

    public ReturnRecordServiceImpl(
            ReturnRecordRepository returnRecordRepository,
            BorrowRequestRepository borrowRequestRepository,
            EquipmentRepository equipmentRepository,
            BorrowStateResolver stateResolver,
            FineStrategyResolver fineStrategyResolver,
            CurrentUser current,
            ApplicationEventPublisher events,
            FeePolicyProperties fees,
            Clock clock) {
        this.fees = fees;
        this.clock = clock;
        this.current = current;
        this.events = events;
        this.returnRecordRepository = returnRecordRepository;
        this.borrowRequestRepository = borrowRequestRepository;
        this.equipmentRepository = equipmentRepository;
        this.stateResolver = stateResolver;
        this.fineStrategyResolver = fineStrategyResolver;
    }

    @Override
    @Transactional
    public ReturnResponseDto returnEquipment(Long borrowRequestId, ReturnRequestDto dto) {
        current.requireOperator();
        BorrowRequest request =
                borrowRequestRepository
                        .findLockedById(borrowRequestId)
                        .orElseThrow(
                                () ->
                                        ResourceNotFoundException.of(
                                                "BorrowRequest", borrowRequestId));

        current.requireIndependentOperator(request);

        var today = LocalDate.now(clock);
        if (dto.getReturnDate() != null && !dto.getReturnDate().equals(today))
            throw new IllegalArgumentException("Return date must be today.");
        if (request.getBorrowDate() != null && today.isBefore(request.getBorrowDate()))
            throw new IllegalArgumentException("Cannot return before borrowing.");

        var inputs = new HashMap<Long, ReturnRequestDto.Inspection>();
        if (dto.getItems() != null) {
            for (var input : dto.getItems()) {
                if (input == null
                        || input.equipmentId() == null
                        || inputs.put(input.equipmentId(), input) != null)
                    throw new IllegalArgumentException(
                            "Each equipment must be inspected exactly once.");
            }
            var expected =
                    request.getItems().stream()
                            .filter(i -> i.getReturnedOn() == null)
                            .map(i -> i.getEquipment().getId())
                            .collect(Collectors.toSet());
            if (inputs.isEmpty()
                    || !expected.containsAll(inputs.keySet())
                    || (!dto.isPartial() && !inputs.keySet().equals(expected)))
                throw new IllegalArgumentException(
                        "Inspect every equipment in this request and no other equipment.");
        } else {
            for (var item : request.getItems())
                if (item.getReturnedOn() == null)
                    inputs.put(
                            item.getEquipment().getId(),
                            new ReturnRequestDto.Inspection(
                                    item.getEquipment().getId(),
                                    dto.getCondition(),
                                    dto.getRemark()));
        }
        if (request.getStatus() != BorrowStatus.BORROWED
                && request.getStatus() != BorrowStatus.OVERDUE)
            throw new InvalidBorrowStateException("Only active loans can be returned.");
        boolean completed =
                request.getItems().stream()
                        .filter(i -> i.getReturnedOn() == null)
                        .allMatch(i -> inputs.containsKey(i.getEquipment().getId()));
        if (completed) stateResolver.resolve(request.getStatus()).returnEquipment(request);
        FineStrategyService strategy = fineStrategyResolver.resolve(request.getUser().getRole());
        BigDecimal fine =
                request.getDailyFine() == null
                        ? strategy.calculate(request, today)
                        : request.getDailyFine()
                                .multiply(
                                        BigDecimal.valueOf(
                                                Math.max(
                                                        0,
                                                        ChronoUnit.DAYS.between(
                                                                        request.getDueDate(), today)
                                                                - request.getGraceDays())));
        ReturnRecord record =
                returnRecordRepository
                        .findByBorrowRequestId(borrowRequestId)
                        .orElseGet(ReturnRecord::new);
        BigDecimal damage =
                record.getDamageAmount() == null ? BigDecimal.ZERO : record.getDamageAmount();
        for (BorrowItem item :
                request.getItems().stream()
                        .filter(i -> inputs.containsKey(i.getEquipment().getId()))
                        .sorted(Comparator.comparing(i -> i.getEquipment().getId()))
                        .toList()) {
            Equipment equipment =
                    equipmentRepository
                            .findLockedById(item.getEquipment().getId())
                            .orElseThrow(
                                    () ->
                                            ResourceNotFoundException.of(
                                                    "Equipment", item.getEquipment().getId()));
            var input = inputs.get(equipment.getId());
            var condition = ReturnCondition.parse(input.condition());
            if (condition != ReturnCondition.NORMAL
                    && (input.remark() == null || input.remark().isBlank()))
                throw new IllegalArgumentException(
                        "Describe the damage or loss for " + equipment.getName() + ".");
            BigDecimal price =
                    item.getSnapshotName() == null
                            ? equipment.getPurchasePrice()
                            : item.getSnapshotPurchasePrice();
            BigDecimal rate =
                    request.getDailyFine() == null
                            ? fees.damageRate(condition)
                            : switch (condition) {
                                case NORMAL -> BigDecimal.ZERO;
                                case MINOR_SCRATCHES -> request.getScratchRate();
                                case DAMAGED -> request.getDamageRate();
                                case LOST -> request.getLossRate();
                            };
            if (condition != ReturnCondition.NORMAL && price == null)
                throw new IllegalArgumentException(
                        "Missing recorded purchase price for "
                                + equipment.getName()
                                + " before charging for damage.");
            if (price != null && price.signum() < 0)
                throw new IllegalArgumentException("Invalid equipment price.");
            BigDecimal charge =
                    price == null
                            ? BigDecimal.ZERO
                            : price.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            damage = damage.add(charge);
            record.getInspections()
                    .add(
                            new ReturnInspection(
                                    equipment.getId(),
                                    item.getSnapshotName() != null
                                            ? item.getSnapshotName()
                                            : equipment.getName(),
                                    condition.name(),
                                    price,
                                    rate,
                                    charge,
                                    input.remark()));
            if (equipment.getStatus() != EquipmentStatus.IN_USE)
                throw new InvalidBorrowStateException("Equipment is not currently in use.");
            equipment.setStatus(condition.getStatus());
            item.setReturnedOn(today);
        }
        var conditions =
                record.getInspections().stream()
                        .map(ReturnInspection::getCondition)
                        .distinct()
                        .toList();
        record.setBorrowRequest(request);
        record.setReturnDate(today);
        record.setCondition(conditions.size() == 1 ? conditions.get(0) : "MIXED");
        record.setFineAmount(fine);
        record.setDamageAmount(damage);
        record.setRemark(dto.getRemark());

        borrowRequestRepository.save(request);
        ReturnRecord saved = returnRecordRepository.save(record);

        events.publishEvent(
                new BorrowWorkflowEvent(
                        request.getId(), completed ? "RETURNED" : "PARTIAL_RETURN"));
        return toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnResponseDto getByBorrowRequestId(Long borrowRequestId) {
        current.requireOwnerOrOperator(
                borrowRequestRepository
                        .findById(borrowRequestId)
                        .orElseThrow(
                                () ->
                                        ResourceNotFoundException.of(
                                                "BorrowRequest", borrowRequestId)));
        ReturnRecord record =
                returnRecordRepository
                        .findByBorrowRequestId(borrowRequestId)
                        .orElseThrow(
                                () ->
                                        ResourceNotFoundException.of(
                                                "ReturnRecord (by borrowRequestId)",
                                                borrowRequestId));
        return toResponseDto(record);
    }

    private ReturnResponseDto toResponseDto(ReturnRecord record) {
        return ReturnResponseDto.builder()
                .id(record.getId())
                .borrowRequestId(record.getBorrowRequest().getId())
                .returnDate(record.getReturnDate())
                .condition(record.getCondition())
                .fineAmount(record.getFineAmount())
                .damageAmount(record.getDamageAmount())
                .items(
                        record.getInspections().stream()
                                .map(ReturnInspectionResponseDto::from)
                                .toList())
                .remark(record.getRemark())
                .build();
    }
}
