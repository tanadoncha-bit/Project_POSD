package com.example.itborrow.service;

import com.example.itborrow.dto.request.ReturnRequestDto;
import com.example.itborrow.dto.response.ReturnResponseDto;

public interface ReturnRecordService {

    ReturnResponseDto returnEquipment(Long borrowRequestId, ReturnRequestDto dto);

    ReturnResponseDto getByBorrowRequestId(Long borrowRequestId);
}