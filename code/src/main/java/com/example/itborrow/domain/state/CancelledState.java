package com.example.itborrow.domain.state;

import com.example.itborrow.domain.entity.BorrowRequest;
import com.example.itborrow.domain.enums.BorrowStatus;
import com.example.itborrow.exception.InvalidBorrowStateException;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class CancelledState implements BorrowState {
    public Set<BorrowStatus> supports() {
        return Set.of(BorrowStatus.CANCELLED);
    }

    @Override
    public void approve(BorrowRequest request) {
        throw new InvalidBorrowStateException("คำขอนี้ปิดงานแล้ว (สถานะปัจจุบัน: CANCELLED)");
    }

    @Override
    public void pickUp(BorrowRequest request) {
        throw new InvalidBorrowStateException("คำขอนี้ปิดงานแล้ว (สถานะปัจจุบัน: CANCELLED)");
    }

    @Override
    public void returnEquipment(BorrowRequest request) {
        throw new InvalidBorrowStateException(
                "คำขอนี้ถูกยกเลิกแล้ว ไม่สามารถคืนอุปกรณ์ได้ (สถานะปัจจุบัน: CANCELLED)");
    }

    @Override
    public void cancel(BorrowRequest request) {
        throw new InvalidBorrowStateException(
                "คำขอนี้ปิดงานแล้ว ไม่สามารถยกเลิกได้ (สถานะปัจจุบัน: CANCELLED)");
    }

    @Override
    public void markOverdue(BorrowRequest request) {
        throw new InvalidBorrowStateException("คำขอนี้ปิดงานแล้ว (สถานะปัจจุบัน: CANCELLED)");
    }
}
