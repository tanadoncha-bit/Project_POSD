package com.example.itborrow.domain.state;

import com.example.itborrow.domain.entity.BorrowRequest;
import com.example.itborrow.domain.enums.BorrowStatus;
import com.example.itborrow.exception.InvalidBorrowStateException;
import org.springframework.stereotype.Component;

/**
 * สถานะ PENDING: รอ Admin อนุมัติ
 * ทำได้แค่ approve() หรือ cancel() เท่านั้น ยังรับของ/คืนของไม่ได้
 */
@Component
public class PendingState implements BorrowState {

    @Override
    public void approve(BorrowRequest request) {
        request.setStatus(BorrowStatus.APPROVED);
    }

    @Override
    public void pickUp(BorrowRequest request) {
        throw new InvalidBorrowStateException(
                "ไม่สามารถรับอุปกรณ์ได้ เพราะคำขอยังไม่ได้รับการอนุมัติ (สถานะปัจจุบัน: PENDING)");
    }

    @Override
    public void returnEquipment(BorrowRequest request) {
        throw new InvalidBorrowStateException(
                "ไม่สามารถคืนอุปกรณ์ได้ เพราะยังไม่เคยยืมออกไป (สถานะปัจจุบัน: PENDING)");
    }

    @Override
    public void cancel(BorrowRequest request) {
        // PENDING ยกเลิกได้เลย ไม่ต้องเปลี่ยนสถานะอุปกรณ์เพราะยังไม่เคยถูกจองจริง
        request.setStatus(BorrowStatus.RETURNED); // ใช้ RETURNED แทนสถานะ "ปิดงาน" เพื่อไม่ต้องเพิ่ม enum ใหม่
    }

    @Override
    public void markOverdue(BorrowRequest request) {
        throw new InvalidBorrowStateException(
                "คำขอที่ยังไม่อนุมัติจะเกินกำหนดคืนไม่ได้ (สถานะปัจจุบัน: PENDING)");
    }
}