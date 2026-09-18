package com.example.itborrow.service;

import com.example.itborrow.dto.request.BorrowRequestDto;
import com.example.itborrow.dto.response.BorrowResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BorrowRequestService {

    BorrowResponseDto createBorrowRequest(BorrowRequestDto dto);

    BorrowResponseDto approveBorrowRequest(Long id);

    BorrowResponseDto pickUpEquipment(Long id);

    BorrowResponseDto cancelBorrowRequest(Long id);

    BorrowResponseDto getById(Long id);

    Page<BorrowResponseDto> findAll(Pageable pageable);

    // เรียกจาก scheduled job ทุกวันเพื่อเช็คว่ามีคำขอไหนเกินกำหนดคืนแล้วบ้าง
    void checkAndMarkOverdue();
}