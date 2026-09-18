package com.example.itborrow.domain.state;

import com.example.itborrow.domain.entity.BorrowRequest;
import com.example.itborrow.domain.enums.BorrowStatus;
import com.example.itborrow.exception.InvalidBorrowStateException;
import org.springframework.stereotype.Component;

@Component
public class OverdueState implements BorrowState {

    @Override
    public void approve(BorrowRequest request) {
        throw new InvalidBorrowStateException(
                "คำขอนี้เลยกำหนดคืนแล้ว ไม่สามารถอนุมัติได้ (สถานะปัจจุบัน: OVERDUE)");
    }

    @Override
    public void pickUp(BorrowRequest request) {
        throw new InvalidBorrowStateException(
                "คำขอนี้เลยกำหนดคืนแล้ว ไม่สามารถรับอุปกรณ์ได้ (สถานะปัจจุบัน: OVERDUE)");
    }

    @Override
    public void returnEquipment(BorrowRequest request) {
        request.setStatus(BorrowStatus.RETURNED);
    }

    @Override
    public void cancel(BorrowRequest request) {
        throw new InvalidBorrowStateException(
                "ไม่สามารถยกเลิกได้ เพราะรับอุปกรณ์ไปแล้วและเลยกำหนดคืน (สถานะปัจจุบัน: OVERDUE)");
    }

    @Override
    public void markOverdue(BorrowRequest request) {
        throw new InvalidBorrowStateException(
                "คำขอนี้ถูกทำเครื่องหมายว่าเกินกำหนดไปแล้ว (สถานะปัจจุบัน: OVERDUE)");
    }
}
