package com.example.itborrow.service.impl;

import com.example.itborrow.common.event.OverdueEvent;
import com.example.itborrow.domain.entity.BorrowItem;
import com.example.itborrow.domain.entity.BorrowRequest;
import com.example.itborrow.domain.entity.Equipment;
import com.example.itborrow.domain.entity.User;
import com.example.itborrow.domain.enums.BorrowStatus;
import com.example.itborrow.domain.enums.EquipmentStatus;
import com.example.itborrow.domain.state.BorrowStateResolver;
import com.example.itborrow.dto.request.BorrowItemRequestDto;
import com.example.itborrow.dto.request.BorrowRequestDto;
import com.example.itborrow.dto.response.BorrowResponseDto;
import com.example.itborrow.exception.EquipmentNotAvailableException;
import com.example.itborrow.exception.ResourceNotFoundException;
import com.example.itborrow.mapper.BorrowRequestMapper;
import com.example.itborrow.repository.BorrowRequestRepository;
import com.example.itborrow.repository.EquipmentRepository;
import com.example.itborrow.service.BorrowRequestService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class BorrowRequestServiceImpl implements BorrowRequestService {

    private final com.example.itborrow.service.CurrentUser current;
    private final BorrowRequestRepository borrowRequestRepository;
    private final EquipmentRepository equipmentRepository;
    private final BorrowStateResolver stateResolver;
    private final BorrowRequestMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    public BorrowRequestServiceImpl(BorrowRequestRepository borrowRequestRepository,
                                     EquipmentRepository equipmentRepository,
                                     BorrowStateResolver stateResolver,
                                     BorrowRequestMapper mapper,
                                     ApplicationEventPublisher eventPublisher, com.example.itborrow.service.CurrentUser current) {
        this.current = current;
        this.borrowRequestRepository = borrowRequestRepository;
        this.equipmentRepository = equipmentRepository;
        this.stateResolver = stateResolver;
        this.mapper = mapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public BorrowResponseDto createBorrowRequest(BorrowRequestDto dto) {
        User user = current.require();
        if (dto.getBorrowDate() == null || dto.getDueDate() == null || dto.getBorrowDate().isBefore(LocalDate.now()) || dto.getDueDate().isBefore(dto.getBorrowDate()))
            throw new IllegalArgumentException("Invalid borrow/due dates.");
        if (dto.getItems() == null || dto.getItems().isEmpty()) throw new IllegalArgumentException("Select equipment.");
        var equipmentIds = new java.util.HashSet<Long>();
        for (var item : dto.getItems()) {
            if (item == null || item.getEquipmentId() == null || item.getQuantity() != 1 || !equipmentIds.add(item.getEquipmentId()))
                throw new IllegalArgumentException("Each asset can appear once, with quantity 1.");
        }

        BorrowRequest request = new BorrowRequest();
        request.setUser(user);
        request.setBorrowDate(dto.getBorrowDate());
        request.setDueDate(dto.getDueDate());
        request.setNote(dto.getNote());
        request.setStatus(BorrowStatus.PENDING);

        for (BorrowItemRequestDto itemDto : dto.getItems()) {
            Equipment equipment = equipmentRepository.findById(itemDto.getEquipmentId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Equipment", itemDto.getEquipmentId()));

            if (equipment.getStatus() != EquipmentStatus.AVAILABLE) {
                throw new EquipmentNotAvailableException(
                        "อุปกรณ์ " + equipment.getAssetCode() + " ไม่พร้อมให้ยืมในขณะนี้ (สถานะ: "
                                + equipment.getStatus() + ")");
            }

            BorrowItem item = new BorrowItem();
            item.setEquipment(equipment);
            item.setQuantity(itemDto.getQuantity());
            request.addItem(item);
        }

        BorrowRequest saved = borrowRequestRepository.save(request);
        return mapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public BorrowResponseDto approveBorrowRequest(Long id) {
        current.requireOperator();
        BorrowRequest request = findLockedById(id);
        current.requireIndependentOperator(request);
        if (request.getDueDate().isBefore(LocalDate.now())) throw new IllegalArgumentException("This borrowing period has expired.");
        for (var item : request.getItems().stream().sorted(java.util.Comparator.comparing(i -> i.getEquipment().getId())).toList()) {
            var asset = equipmentRepository.findLockedById(item.getEquipment().getId()).orElseThrow();
            if (asset.getStatus() != EquipmentStatus.AVAILABLE || borrowRequestRepository.countConflicts(asset.getId(), request.getId(), request.getBorrowDate(), request.getDueDate(), java.util.List.of(BorrowStatus.APPROVED, BorrowStatus.BORROWED, BorrowStatus.OVERDUE)) > 0)
                throw new EquipmentNotAvailableException("Equipment is unavailable or reserved during this period: " + asset.getAssetCode());
        }
        stateResolver.resolve(request.getStatus()).approve(request);
        return mapper.toResponseDto(borrowRequestRepository.save(request));
    }

    @Override
    @Transactional
    public BorrowResponseDto pickUpEquipment(Long id) {
        current.requireOperator();
        BorrowRequest request = findLockedById(id);
        current.requireIndependentOperator(request);
        if (LocalDate.now().isBefore(request.getBorrowDate()) || LocalDate.now().isAfter(request.getDueDate()))
            throw new IllegalArgumentException("Pickup must be within the requested borrowing period.");
        stateResolver.resolve(request.getStatus()).pickUp(request);

        for (BorrowItem item : request.getItems().stream().sorted(java.util.Comparator.comparing(i -> i.getEquipment().getId())).toList()) {
            Equipment equipment = item.getEquipment();
            if (equipmentRepository.transition(equipment.getId(), EquipmentStatus.AVAILABLE, EquipmentStatus.IN_USE) != 1)
                throw new EquipmentNotAvailableException("Equipment is no longer available: " + equipment.getAssetCode());
            equipment.setStatus(EquipmentStatus.IN_USE);
        }

        return mapper.toResponseDto(borrowRequestRepository.save(request));
    }

    @Override
    @Transactional
    public BorrowResponseDto cancelBorrowRequest(Long id) {
        BorrowRequest request = findLockedById(id);
        current.requireOwnerOrOperator(request);
        stateResolver.resolve(request.getStatus()).cancel(request);
        return mapper.toResponseDto(borrowRequestRepository.save(request));
    }

    @Override
    @Transactional(readOnly = true)
    public BorrowResponseDto getById(Long id) {
        var request = findEntityById(id);
        current.requireOwnerOrOperator(request);
        return mapper.toResponseDto(request);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BorrowResponseDto> findAll(Pageable pageable) {
        var user=current.require();
        return (current.isOperator(user) ? borrowRequestRepository.findAll(pageable) : borrowRequestRepository.findByUserId(user.getId(),pageable)).map(mapper::toResponseDto);
    }

    @Override
    @Transactional
    public void checkAndMarkOverdue() {
        List<BorrowRequest> overdueCandidates = borrowRequestRepository
                .findOverdueForUpdate(BorrowStatus.BORROWED, LocalDate.now());

        for (BorrowRequest request : overdueCandidates) {
            stateResolver.resolve(request.getStatus()).markOverdue(request);
            borrowRequestRepository.save(request);

            eventPublisher.publishEvent(new OverdueEvent(this, request));
        }
    }

    private BorrowRequest findLockedById(Long id) {
        return borrowRequestRepository.findLockedById(id).orElseThrow(() -> ResourceNotFoundException.of("BorrowRequest", id));
    }

    private BorrowRequest findEntityById(Long id) {
        return borrowRequestRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("BorrowRequest", id));
    }
}