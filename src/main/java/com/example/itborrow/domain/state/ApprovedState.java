package com.example.itborrow.domain.state;

import com.example.itborrow.domain.entity.BorrowRequest;
import com.example.itborrow.domain.enums.BorrowStatus;
import com.example.itborrow.exception.InvalidBorrowStateException;
import org.springframework.stereotype.Component;

@Component
public class ApprovedState implements BorrowState {

    @Override
    public void approve(BorrowRequest request) {
        throw new InvalidBorrowStateException(
                "คำขอนี้ได้รับการอนุมัติไปแล้ว ไม่สามารถอนุมัติซ้ำได้ (สถานะปัจจุบัน: APPROVED)");
    }

    @Override
    public void pickUp(BorrowRequest request) {
        request.setStatus(BorrowStatus.BORROWED);
    }

    @Override
    public void returnEquipment(BorrowRequest request) {
        throw new InvalidBorrowStateException(
                "ไม่สามารถคืนอุปกรณ์ได้ เพราะยังไม่ได้รับอุปกรณ์ไปจริง (สถานะปัจจุบัน: APPROVED)");
    }

    @Override
    public void cancel(BorrowRequest request) {
        request.setStatus(BorrowStatus.RETURNED);
    }

    @Override
    public void markOverdue(BorrowRequest request) {
        throw new InvalidBorrowStateException(
                "คำขอที่ยังไม่ได้รับอุปกรณ์จะเกินกำหนดคืนไม่ได้ (สถานะปัจจุบัน: APPROVED)");
    }
}