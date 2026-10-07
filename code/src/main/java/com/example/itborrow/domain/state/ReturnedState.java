package com.example.itborrow.domain.state;

import com.example.itborrow.domain.entity.BorrowRequest;
import com.example.itborrow.exception.InvalidBorrowStateException;
import org.springframework.stereotype.Component;

@Component
public class ReturnedState implements BorrowState {
    public java.util.Set<com.example.itborrow.domain.enums.BorrowStatus> supports() {
        return java.util.Set.of(com.example.itborrow.domain.enums.BorrowStatus.RETURNED, com.example.itborrow.domain.enums.BorrowStatus.CANCELLED);
    }


    @Override
    public void approve(BorrowRequest request) {
        throw new InvalidBorrowStateException("คำขอนี้ปิดงานแล้ว (สถานะปัจจุบัน: RETURNED)");
    }

    @Override
    public void pickUp(BorrowRequest request) {
        throw new InvalidBorrowStateException("คำขอนี้ปิดงานแล้ว (สถานะปัจจุบัน: RETURNED)");
    }

    @Override
    public void returnEquipment(BorrowRequest request) {
        throw new InvalidBorrowStateException("คำขอนี้ถูกคืนไปแล้ว ไม่สามารถคืนซ้ำได้ (สถานะปัจจุบัน: RETURNED)");
    }

    @Override
    public void cancel(BorrowRequest request) {
        throw new InvalidBorrowStateException("คำขอนี้ปิดงานแล้ว ไม่สามารถยกเลิกได้ (สถานะปัจจุบัน: RETURNED)");
    }

    @Override
    public void markOverdue(BorrowRequest request) {
        throw new InvalidBorrowStateException("คำขอนี้ปิดงานแล้ว (สถานะปัจจุบัน: RETURNED)");
    }
}