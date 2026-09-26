package com.example.itborrow.service;

import com.example.itborrow.domain.entity.BorrowItem;
import com.example.itborrow.domain.entity.BorrowRequest;
import com.example.itborrow.domain.entity.Equipment;
import com.example.itborrow.domain.entity.ReturnRecord;
import com.example.itborrow.domain.entity.User;
import com.example.itborrow.domain.enums.BorrowStatus;
import com.example.itborrow.domain.enums.EquipmentStatus;
import com.example.itborrow.domain.enums.Role;
import com.example.itborrow.domain.state.BorrowState;
import com.example.itborrow.domain.state.BorrowStateResolver;
import com.example.itborrow.dto.request.ReturnRequestDto;
import com.example.itborrow.exception.ResourceNotFoundException;
import com.example.itborrow.repository.BorrowRequestRepository;
import com.example.itborrow.repository.EquipmentRepository;
import com.example.itborrow.repository.ReturnRecordRepository;
import com.example.itborrow.service.impl.ReturnRecordServiceImpl;
import com.example.itborrow.service.impl.strategy.FinestrategyResolver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReturnRecordServiceTest {

    @Mock private ReturnRecordRepository returnRecordRepository;
    @Mock private BorrowRequestRepository borrowRequestRepository;
    @Mock private EquipmentRepository equipmentRepository;
    @Mock private BorrowStateResolver stateResolver;
    @Mock private FinestrategyResolver fineStrategyResolver;
    @Mock private BorrowState mockState;
    @Mock private FineStrategyService mockStrategy;

    @InjectMocks
    private ReturnRecordServiceImpl service;

    private BorrowRequest borrowRequest;
    private Equipment equipment;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.STAFF);

        equipment = new Equipment();
        equipment.setId(10L);
        equipment.setStatus(EquipmentStatus.IN_USE);

        BorrowItem item = new BorrowItem();
        item.setEquipment(equipment);
        item.setQuantity(1);

        borrowRequest = new BorrowRequest();
        borrowRequest.setId(1L);
        borrowRequest.setUser(user);
        borrowRequest.setDueDate(LocalDate.now().minusDays(2)); // เลยกำหนดมาแล้ว 2 วัน
        borrowRequest.setStatus(BorrowStatus.OVERDUE);
        borrowRequest.addItem(item);
    }

    @Test
    void returnEquipment_success_callsStateAndStrategyThenSavesRecord() {
        ReturnRequestDto dto = new ReturnRequestDto();
        dto.setReturnDate(LocalDate.now());
        dto.setCondition("GOOD");

        when(borrowRequestRepository.findById(1L)).thenReturn(Optional.of(borrowRequest));
        when(stateResolver.resolve(BorrowStatus.OVERDUE)).thenReturn(mockState);
        when(fineStrategyResolver.resolve(Role.STAFF)).thenReturn(mockStrategy);
        when(mockStrategy.calculate(eq(borrowRequest), any(LocalDate.class)))
                .thenReturn(BigDecimal.valueOf(100)); // สมมติค่าปรับ 100 บาท
        when(returnRecordRepository.save(any(ReturnRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.returnEquipment(1L, dto);

        verify(mockState).returnEquipment(borrowRequest);

        assertThat(result.getFineAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(equipment.getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
        verify(returnRecordRepository).save(any(ReturnRecord.class));
    }

    @Test
    void returnEquipment_throwsException_whenBorrowRequestNotFound() {
        ReturnRequestDto dto = new ReturnRequestDto();
        dto.setReturnDate(LocalDate.now());

        when(borrowRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.returnEquipment(99L, dto))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(returnRecordRepository, never()).save(any());
    }

    @Test
    void returnEquipment_noFine_whenReturnedOnTime() {
        borrowRequest.setDueDate(LocalDate.now().plusDays(1));
        borrowRequest.setStatus(BorrowStatus.BORROWED);

        ReturnRequestDto dto = new ReturnRequestDto();
        dto.setReturnDate(LocalDate.now());
        dto.setCondition("GOOD");

        when(borrowRequestRepository.findById(1L)).thenReturn(Optional.of(borrowRequest));
        when(stateResolver.resolve(BorrowStatus.BORROWED)).thenReturn(mockState);
        when(fineStrategyResolver.resolve(Role.STAFF)).thenReturn(mockStrategy);
        when(mockStrategy.calculate(eq(borrowRequest), any(LocalDate.class)))
                .thenReturn(BigDecimal.ZERO);
        when(returnRecordRepository.save(any(ReturnRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.returnEquipment(1L, dto);

        assertThat(result.getFineAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getByBorrowRequestId_throwsException_whenNotReturnedYet() {
        when(returnRecordRepository.findByBorrowRequestId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByBorrowRequestId(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}