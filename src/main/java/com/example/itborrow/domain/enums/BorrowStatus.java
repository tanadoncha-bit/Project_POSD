package com.example.itborrow.domain.enums;

/**
 * สถานะของ BorrowRequest ตลอด lifecycle การยืม-คืน
 * ใช้คู่กับ State Pattern (domain.state.BorrowState และ implementation ทั้ง 5 ตัว)
 */
public enum BorrowStatus {
    PENDING,    // รอการอนุมัติจาก Admin
    APPROVED,   // อนุมัติแล้ว รอผู้ยืมมารับอุปกรณ์
    BORROWED,   // รับอุปกรณ์ไปแล้ว กำลังอยู่ในความครอบครอง
    RETURNED,   // คืนอุปกรณ์เรียบร้อย ปิดงานแล้ว
    OVERDUE     // เลยกำหนดคืนแล้วแต่ยังไม่คืน
}