package com.example.itborrow.controller.api;

import com.example.itborrow.dto.request.BorrowRequestDto;
import com.example.itborrow.dto.request.ReturnRequestDto;
import com.example.itborrow.dto.response.BorrowResponseDto;
import com.example.itborrow.dto.response.ReturnResponseDto;
import com.example.itborrow.service.BorrowRequestService;
import com.example.itborrow.service.ReturnRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/borrow-requests")
@Tag(name = "Borrow Request", description = "จัดการคำขอยืม-คืนอุปกรณ์ IT")
public class BorrowRequestController {

    private final BorrowRequestService borrowRequestService;
    private final ReturnRecordService returnRecordService;

    public BorrowRequestController(BorrowRequestService borrowRequestService,
                                    ReturnRecordService returnRecordService) {
        this.borrowRequestService = borrowRequestService;
        this.returnRecordService = returnRecordService;
    }

    @Operation(summary = "สร้างคำขอยืมอุปกรณ์ใหม่")
    @PostMapping
    public ResponseEntity<BorrowResponseDto> create(@Valid @RequestBody BorrowRequestDto dto) {
        BorrowResponseDto response = borrowRequestService.createBorrowRequest(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "ดูรายละเอียดคำขอยืมตาม id")
    @GetMapping("/{id}")
    public ResponseEntity<BorrowResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(borrowRequestService.getById(id));
    }

    @Operation(summary = "ดูคำขอยืมทั้งหมด พร้อม pagination และ sorting")
    @GetMapping
    public ResponseEntity<Page<BorrowResponseDto>> getAll(Pageable pageable) {
        return ResponseEntity.ok(borrowRequestService.findAll(pageable));
    }

    @Operation(summary = "Admin อนุมัติคำขอยืม: PENDING -> APPROVED")
    @PatchMapping("/{id}/approve")
    public ResponseEntity<BorrowResponseDto> approve(@PathVariable Long id) {
        return ResponseEntity.ok(borrowRequestService.approveBorrowRequest(id));
    }

    @Operation(summary = "ผู้ยืมมารับอุปกรณ์จริง: APPROVED -> BORROWED")
    @PatchMapping("/{id}/pickup")
    public ResponseEntity<BorrowResponseDto> pickUp(@PathVariable Long id) {
        return ResponseEntity.ok(borrowRequestService.pickUpEquipment(id));
    }

    @Operation(summary = "ยกเลิกคำขอยืม (ทำได้ก่อนรับอุปกรณ์เท่านั้น)")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BorrowResponseDto> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(borrowRequestService.cancelBorrowRequest(id));
    }

    @Operation(summary = "คืนอุปกรณ์: BORROWED/OVERDUE -> RETURNED พร้อมคำนวณค่าปรับ")
    @PostMapping("/{id}/return")
    public ResponseEntity<ReturnResponseDto> returnEquipment(@PathVariable Long id,@Valid @RequestBody ReturnRequestDto dto) {
        return ResponseEntity.ok(returnRecordService.returnEquipment(id, dto));
    }

    @Operation(summary = "ดูข้อมูลการคืนของคำขอยืมนี้ (ถ้ามี)")
    @GetMapping("/{id}/return")
    public ResponseEntity<ReturnResponseDto> getReturnInfo(@PathVariable Long id) {
        return ResponseEntity.ok(returnRecordService.getByBorrowRequestId(id));
    }
}