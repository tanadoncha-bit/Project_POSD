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
import com.example.itborrow.repository.UserRepository;
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

    private final BorrowRequestRepository borrowRequestRepository;
    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;
    private final BorrowStateResolver stateResolver;
    private final BorrowRequestMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    // Constructor Injection ล้วน — ไม่ใช้ lombok, ไม่ใช้ field injection (@Autowired บน field)
    public BorrowRequestServiceImpl(BorrowRequestRepository borrowRequestRepository,
                                     UserRepository userRepository,
                                     EquipmentRepository equipmentRepository,
                                     BorrowStateResolver stateResolver,
                                     BorrowRequestMapper mapper,
                                     ApplicationEventPublisher eventPublisher) {
        this.borrowRequestRepository = borrowRequestRepository;
        this.userRepository = userRepository;
        this.equipmentRepository = equipmentRepository;
        this.stateResolver = stateResolver;
        this.mapper = mapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public BorrowResponseDto createBorrowRequest(BorrowRequestDto dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", dto.getUserId()));

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
        BorrowRequest request = findEntityById(id);
        stateResolver.resolve(request.getStatus()).approve(request);
        return mapper.toResponseDto(borrowRequestRepository.save(request));
    }

    @Override
    @Transactional
    public BorrowResponseDto pickUpEquipment(Long id) {
        BorrowRequest request = findEntityById(id);
        stateResolver.resolve(request.getStatus()).pickUp(request);

        // ผู้ยืมรับอุปกรณ์จริงแล้ว เปลี่ยนสถานะอุปกรณ์ทุกชิ้นเป็น BORROWED
        for (BorrowItem item : request.getItems()) {
            Equipment equipment = item.getEquipment();
            equipment.setStatus(EquipmentStatus.BORROWED);
            equipmentRepository.save(equipment);
        }

        return mapper.toResponseDto(borrowRequestRepository.save(request));
    }

    @Override
    @Transactional
    public BorrowResponseDto cancelBorrowRequest(Long id) {
        BorrowRequest request = findEntityById(id);
        stateResolver.resolve(request.getStatus()).cancel(request);
        return mapper.toResponseDto(borrowRequestRepository.save(request));
    }

    @Override
    public BorrowResponseDto getById(Long id) {
        return mapper.toResponseDto(findEntityById(id));
    }

    @Override
    public Page<BorrowResponseDto> findAll(Pageable pageable) {
        return borrowRequestRepository.findAll(pageable).map(mapper::toResponseDto);
    }

    @Override
    @Transactional
    public void checkAndMarkOverdue() {
        List<BorrowRequest> overdueCandidates = borrowRequestRepository
                .findByStatusAndDueDateBefore(BorrowStatus.BORROWED, LocalDate.now());

        for (BorrowRequest request : overdueCandidates) {
            stateResolver.resolve(request.getStatus()).markOverdue(request);
            borrowRequestRepository.save(request);

            // Observer Pattern: ยิง event ออกไป ไม่ต้องรู้ว่าใครฟังอยู่บ้าง
            eventPublisher.publishEvent(new OverdueEvent(this, request));
        }
    }

    private BorrowRequest findEntityById(Long id) {
        return borrowRequestRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("BorrowRequest", id));
    }
}