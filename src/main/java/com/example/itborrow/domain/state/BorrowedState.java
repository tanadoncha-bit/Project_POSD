package com.example.itborrow.domain.state;

import com.example.itborrow.domain.entity.BorrowRequest;
import com.example.itborrow.domain.enums.BorrowStatus;
import com.example.itborrow.exception.InvalidBorrowStateException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class BorrowedState implements BorrowState {

    @Override
    public void approve(BorrowRequest request) {
        throw new InvalidBorrowStateException(
                "คำขอนี้อยู่ระหว่างการยืม ไม่สามารถอนุมัติซ้ำได้ (สถานะปัจจุบัน: BORROWED)");
    }

    @Override
    public void pickUp(BorrowRequest request) {
        throw new InvalidBorrowStateException(
                "รับอุปกรณ์ไปแล้ว ไม่สามารถรับซ้ำได้ (สถานะปัจจุบัน: BORROWED)");
    }

    @Override
    public void returnEquipment(BorrowRequest request) {
        request.setStatus(BorrowStatus.RETURNED);
    }

    @Override
    public void cancel(BorrowRequest request) {
        throw new InvalidBorrowStateException(
                "ไม่สามารถยกเลิกได้ เพราะรับอุปกรณ์ไปแล้ว ต้องทำเรื่องคืนแทน (สถานะปัจจุบัน: BORROWED)");
    }

    @Override
    public void markOverdue(BorrowRequest request) {
        if (request.getDueDate().isBefore(LocalDate.now())) {
            request.setStatus(BorrowStatus.OVERDUE);
        }
    }
}