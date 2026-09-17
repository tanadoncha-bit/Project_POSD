package com.example.itborrow.domain.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "return_records")
public class ReturnRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK -> BorrowRequest (ฝั่งเป็นเจ้าของความสัมพันธ์ 1:1)
    // unique = true เพื่อบังคับว่า BorrowRequest 1 ใบมี ReturnRecord ได้แค่ 1 แถวเท่านั้น
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "borrow_request_id", nullable = false, unique = true)
    private BorrowRequest borrowRequest;

    @Column(name = "return_date", nullable = false)
    private LocalDate returnDate;

    // สภาพอุปกรณ์ตอนคืน เช่น GOOD, DAMAGED, LOST
    @Column(name = "condition", length = 50, nullable = false)
    private String condition;

    // คำนวณโดย FineStrategyService ตาม role ของผู้ยืม (Standard / VIP)
    @Column(name = "fine_amount", precision = 10, scale = 2)
    private BigDecimal fineAmount = BigDecimal.ZERO;

    @Column(name = "remark", length = 500)
    private String remark;


    // NoArgsConstructor
    public ReturnRecord() {
    }

    // AllArgsConstructor
    public ReturnRecord(
            Long id,
            BorrowRequest borrowRequest,
            LocalDate returnDate,
            String condition,
            BigDecimal fineAmount,
            String remark
    ) {
        this.id = id;
        this.borrowRequest = borrowRequest;
        this.returnDate = returnDate;
        this.condition = condition;
        this.fineAmount = fineAmount;
        this.remark = remark;
    }


    // Getters

    public Long getId() {
        return id;
    }

    public BorrowRequest getBorrowRequest() {
        return borrowRequest;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public String getCondition() {
        return condition;
    }

    public BigDecimal getFineAmount() {
        return fineAmount;
    }

    public String getRemark() {
        return remark;
    }


    // Setters

    public void setId(Long id) {
        this.id = id;
    }

    public void setBorrowRequest(BorrowRequest borrowRequest) {
        this.borrowRequest = borrowRequest;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public void setFineAmount(BigDecimal fineAmount) {
        this.fineAmount = fineAmount;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}