package com.example.itborrow.domain.state;

import com.example.itborrow.domain.entity.BorrowRequest;
import com.example.itborrow.exception.InvalidBorrowStateException;
import org.springframework.stereotype.Component;

/**
 * สถานะ RETURNED: ปิดงานแล้ว เป็น final state
 * ทุก action ที่เปลี่ยนสถานะต่อจากนี้ต้อง throw exception หมด
 * เพราะ BorrowRequest ที่คืนแล้วห้ามแก้ไข (Liskov Substitution:
 * สถานะนี้ยังคง "ใช้แทน BorrowState ได้" โดยไม่ throw UnsupportedOperationException
 * แต่ throw business exception ที่มีความหมายแทน)
 */
@Component
public class ReturnedState implements BorrowState {

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