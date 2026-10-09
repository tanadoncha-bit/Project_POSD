package com.example.itborrow.domain.state;

import com.example.itborrow.domain.entity.BorrowRequest;
import com.example.itborrow.domain.enums.BorrowStatus;
import com.example.itborrow.exception.InvalidBorrowStateException;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PendingState implements BorrowState {
    public Set<BorrowStatus> supports() {
        return Set.of(BorrowStatus.PENDING);
    }

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
        request.setStatus(BorrowStatus.CANCELLED);
    }

    @Override
    public void markOverdue(BorrowRequest request) {
        throw new InvalidBorrowStateException(
                "คำขอที่ยังไม่อนุมัติจะเกินกำหนดคืนไม่ได้ (สถานะปัจจุบัน: PENDING)");
    }
}
