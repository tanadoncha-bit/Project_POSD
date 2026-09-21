package com.example.itborrow.dto.request;

public class EquipmentRequestDto {
    private String name;
    private String status;
    // หากมีฟิลด์อื่นเช่น serialNumber, type ให้ใส่เพิ่มที่นี่

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}