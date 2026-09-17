package com.example.itborrow.domain.state;

import com.example.itborrow.domain.entity.BorrowRequest;

/**
 * State Pattern: กำหนดพฤติกรรมของ BorrowRequest ในแต่ละสถานะ
 * BorrowRequestServiceImpl จะเรียก state.approve(...) โดยไม่ต้องรู้ว่าตอนนี้อยู่สถานะไหน
 * — ลบ if-else/switch(status) ยาวๆ ออกจาก Service ตามหลัก Open/Closed Principle
 * (เพิ่มสถานะใหม่ = เพิ่มคลาส implement ตัวนี้ ไม่ต้องแก้โค้ดเดิม)
 */
public interface BorrowState {

    /** อนุมัติคำขอยืม: PENDING -> APPROVED เท่านั้นที่ทำได้จริง สถานะอื่น throw exception */
    void approve(BorrowRequest request);

    /** ผู้ยืมมารับอุปกรณ์จริง: APPROVED -> BORROWED */
    void pickUp(BorrowRequest request);

    /** คืนอุปกรณ์: BORROWED หรือ OVERDUE -> RETURNED */
    void returnEquipment(BorrowRequest request);

    /** ยกเลิกคำขอ: ทำได้เฉพาะก่อนรับของจริง (PENDING/APPROVED) */
    void cancel(BorrowRequest request);

    /** ระบบตรวจพบว่าเลยกำหนดคืนแล้ว: BORROWED -> OVERDUE */
    void markOverdue(BorrowRequest request);
}