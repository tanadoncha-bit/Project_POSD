package com.example.itborrow.service;

import com.example.itborrow.domain.entity.BorrowRequest;
import com.example.itborrow.domain.entity.Equipment;
import com.example.itborrow.domain.entity.User;
import com.example.itborrow.domain.enums.BorrowStatus;
import com.example.itborrow.domain.enums.EquipmentStatus;
import com.example.itborrow.domain.state.BorrowState;
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
import com.example.itborrow.service.impl.BorrowRequestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BorrowRequestServiceTest {

    @Mock private BorrowRequestRepository borrowRequestRepository;
    @Mock private UserRepository userRepository;
    @Mock private EquipmentRepository equipmentRepository;
    @Mock private BorrowStateResolver stateResolver;
    @Mock private BorrowRequestMapper mapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private BorrowState mockState;

    @InjectMocks
    private BorrowRequestServiceImpl service;

    private User testUser;
    private Equipment testEquipment;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("somchai");

        testEquipment = new Equipment();
        testEquipment.setId(10L);
        testEquipment.setAssetCode("LAP-001");
        testEquipment.setName("Dell Latitude 5420");
        testEquipment.setStatus(EquipmentStatus.AVAILABLE);
    }

    // ===== createBorrowRequest =====

    @Test
    void createBorrowRequest_success_whenEquipmentAvailable() {
        BorrowItemRequestDto itemDto = new BorrowItemRequestDto(10L, 1);
        BorrowRequestDto dto = new BorrowRequestDto(1L, LocalDate.now().plusDays(7), "ยืมไปประชุม", List.of(itemDto));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(testEquipment));
        when(borrowRequestRepository.save(any(BorrowRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponseDto(any(BorrowRequest.class))).thenReturn(new BorrowResponseDto());

        BorrowResponseDto result = service.createBorrowRequest(dto);

        assertThat(result).isNotNull();
        verify(borrowRequestRepository).save(any(BorrowRequest.class));
    }

    @Test
    void createBorrowRequest_throwsException_whenEquipmentNotAvailable() {
        testEquipment.setStatus(EquipmentStatus.BORROWED); // อุปกรณ์ถูกยืมอยู่แล้ว

        BorrowItemRequestDto itemDto = new BorrowItemRequestDto(10L, 1);
        BorrowRequestDto dto = new BorrowRequestDto(1L, LocalDate.now().plusDays(7), null, List.of(itemDto));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(testEquipment));

        assertThatThrownBy(() -> service.createBorrowRequest(dto))
                .isInstanceOf(EquipmentNotAvailableException.class)
                .hasMessageContaining("ไม่พร้อมให้ยืม");

        // ต้องไม่มีการ save เกิดขึ้นเลยเมื่ออุปกรณ์ไม่พร้อม
        verify(borrowRequestRepository, never()).save(any());
    }

    @Test
    void createBorrowRequest_throwsException_whenUserNotFound() {
        BorrowRequestDto dto = new BorrowRequestDto(999L, LocalDate.now().plusDays(7), null, Collections.emptyList());
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createBorrowRequest(dto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ===== approveBorrowRequest =====

    @Test
    void approveBorrowRequest_success_delegatesToStatePattern() {
        BorrowRequest request = buildBorrowRequest(BorrowStatus.PENDING);

        when(borrowRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(stateResolver.resolve(BorrowStatus.PENDING)).thenReturn(mockState);
        when(borrowRequestRepository.save(request)).thenReturn(request);
        when(mapper.toResponseDto(request)).thenReturn(new BorrowResponseDto());

        service.approveBorrowRequest(1L);

        // ยืนยันว่า Service เรียก state.approve() จริง ไม่ได้เปลี่ยนสถานะเอง
        verify(mockState).approve(request);
    }

    @Test
    void approveBorrowRequest_throwsException_whenRequestNotFound() {
        when(borrowRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approveBorrowRequest(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ===== pickUpEquipment =====

    @Test
    void pickUpEquipment_success_changesEquipmentStatusToBorrowed() {
        BorrowRequest request = buildBorrowRequestWithItem(BorrowStatus.APPROVED);

        when(borrowRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(stateResolver.resolve(BorrowStatus.APPROVED)).thenReturn(mockState);
        when(borrowRequestRepository.save(request)).thenReturn(request);
        when(mapper.toResponseDto(request)).thenReturn(new BorrowResponseDto());

        service.pickUpEquipment(1L);

        // อุปกรณ์ทุกชิ้นในคำขอต้องถูกเปลี่ยนเป็น BORROWED และถูก save
        assertThat(testEquipment.getStatus()).isEqualTo(EquipmentStatus.BORROWED);
        verify(equipmentRepository).save(testEquipment);
    }

    // ===== checkAndMarkOverdue =====

    @Test
    void checkAndMarkOverdue_publishesEventForEachOverdueRequest() {
        BorrowRequest overdueCandidate = buildBorrowRequest(BorrowStatus.BORROWED);
        overdueCandidate.setDueDate(LocalDate.now().minusDays(3));

        when(borrowRequestRepository.findByStatusAndDueDateBefore(eq(BorrowStatus.BORROWED), any(LocalDate.class)))
                .thenReturn(List.of(overdueCandidate));
        when(stateResolver.resolve(BorrowStatus.BORROWED)).thenReturn(mockState);
        when(borrowRequestRepository.save(overdueCandidate)).thenReturn(overdueCandidate);

        service.checkAndMarkOverdue();

        verify(mockState).markOverdue(overdueCandidate);
        // Observer Pattern: ต้องมีการยิง event ออกไปพอดี 1 ครั้ง ตามจำนวนคำขอที่เกินกำหนด
        verify(eventPublisher, times(1)).publishEvent(any());
    }

    @Test
    void checkAndMarkOverdue_doesNothing_whenNoOverdueRequests() {
        when(borrowRequestRepository.findByStatusAndDueDateBefore(eq(BorrowStatus.BORROWED), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        service.checkAndMarkOverdue();

        verify(eventPublisher, never()).publishEvent(any());
    }

    // ===== Helper methods =====

    private BorrowRequest buildBorrowRequest(BorrowStatus status) {
        BorrowRequest request = new BorrowRequest();
        request.setId(1L);
        request.setUser(testUser);
        request.setBorrowDate(LocalDate.now());
        request.setDueDate(LocalDate.now().plusDays(7));
        request.setStatus(status);
        return request;
    }

    private BorrowRequest buildBorrowRequestWithItem(BorrowStatus status) {
        BorrowRequest request = buildBorrowRequest(status);
        var item = new com.example.itborrow.domain.entity.BorrowItem();
        item.setEquipment(testEquipment);
        item.setQuantity(1);
        request.addItem(item);
        return request;
    }
}