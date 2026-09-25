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

    @Test
    void createBorrowRequest_success_whenEquipmentAvailable() {
        BorrowItemRequestDto itemDto = new BorrowItemRequestDto(10L, 1);
        BorrowRequestDto dto = buildBorrowRequestDto(1L, LocalDate.now().plusDays(7), "ยืมไปประชุม", List.of(itemDto));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(testEquipment));
        when(borrowRequestRepository.save(any(BorrowRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponseDto(any(BorrowRequest.class))).thenReturn(BorrowResponseDto.builder().build());

        BorrowResponseDto result = service.createBorrowRequest(dto);

        assertThat(result).isNotNull();
        verify(borrowRequestRepository).save(any(BorrowRequest.class));
    }

    @Test
    void createBorrowRequest_throwsException_whenEquipmentNotAvailable() {
        testEquipment.setStatus(EquipmentStatus.IN_USE);

        BorrowItemRequestDto itemDto = new BorrowItemRequestDto(10L, 1);
        BorrowRequestDto dto = buildBorrowRequestDto(1L, LocalDate.now().plusDays(7), null, List.of(itemDto));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(testEquipment));

        assertThatThrownBy(() -> service.createBorrowRequest(dto))
                .isInstanceOf(EquipmentNotAvailableException.class)
                .hasMessageContaining("ไม่พร้อมให้ยืม");

        verify(borrowRequestRepository, never()).save(any());
    }

    @Test
    void createBorrowRequest_throwsException_whenUserNotFound() {
        BorrowRequestDto dto = buildBorrowRequestDto(999L, LocalDate.now().plusDays(7), null, Collections.emptyList());
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createBorrowRequest(dto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void approveBorrowRequest_success_delegatesToStatePattern() {
        BorrowRequest request = buildBorrowRequest(BorrowStatus.PENDING);

        when(borrowRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(stateResolver.resolve(BorrowStatus.PENDING)).thenReturn(mockState);
        when(borrowRequestRepository.save(request)).thenReturn(request);
        when(mapper.toResponseDto(request)).thenReturn(BorrowResponseDto.builder().build());

        service.approveBorrowRequest(1L);

        verify(mockState).approve(request);
    }

    @Test
    void approveBorrowRequest_throwsException_whenRequestNotFound() {
        when(borrowRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approveBorrowRequest(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void pickUpEquipment_success_changesEquipmentStatusToBorrowed() {
        BorrowRequest request = buildBorrowRequestWithItem(BorrowStatus.APPROVED);

        when(borrowRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(stateResolver.resolve(BorrowStatus.APPROVED)).thenReturn(mockState);
        when(borrowRequestRepository.save(request)).thenReturn(request);
        when(mapper.toResponseDto(request)).thenReturn(BorrowResponseDto.builder().build());

        service.pickUpEquipment(1L);

        assertThat(testEquipment.getStatus()).isEqualTo(EquipmentStatus.IN_USE);
        verify(equipmentRepository).save(testEquipment);
    }

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
        verify(eventPublisher, times(1)).publishEvent(any());
    }

    @Test
    void checkAndMarkOverdue_doesNothing_whenNoOverdueRequests() {
        when(borrowRequestRepository.findByStatusAndDueDateBefore(eq(BorrowStatus.BORROWED), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        service.checkAndMarkOverdue();

        verify(eventPublisher, never()).publishEvent(any());
    }

    private BorrowRequestDto buildBorrowRequestDto(Long userId, LocalDate dueDate, String note,
                                                    List<BorrowItemRequestDto> items) {
        BorrowRequestDto dto = new BorrowRequestDto();
        dto.setUserId(userId);
        dto.setBorrowDate(LocalDate.now());
        dto.setDueDate(dueDate);
        dto.setNote(note);
        dto.setItems(items);
        return dto;
    }

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