package com.example.itborrow.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public class BorrowRequestDto {

    @NotNull(message = "userId ห้ามว่าง")
    private Long userId;

    @NotNull(message = "borrowDate ห้ามว่าง")
    @FutureOrPresent(message = "borrowDate ต้องไม่ใช่วันที่ผ่านมาแล้ว")
    private LocalDate borrowDate;

    @NotNull(message = "dueDate ห้ามว่าง")
    private LocalDate dueDate;

    @Size(max = 500, message = "note ต้องไม่เกิน 500 ตัวอักษร")
    private String note;

    @NotEmpty(message = "ต้องมีอุปกรณ์อย่างน้อย 1 ชิ้นในคำขอ")
    @Valid
    private List<BorrowItemRequestDto> items;

    public BorrowRequestDto() {
    }

    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDate getBorrowDate() {
        return borrowDate;
    }
    public void setBorrowDate(LocalDate borrowDate) {
        this.borrowDate = borrowDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getNote() {
        return note;
    }
    public void setNote(String note) {
        this.note = note;
    }

    public List<BorrowItemRequestDto> getItems() {
        return items;
    }
    public void setItems(List<BorrowItemRequestDto> items) {
        this.items = items;
    }
}