package com.example.itborrow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class ReturnRequestDto {

    @NotNull(message = "returnDate ห้ามว่าง")
    private LocalDate returnDate;

    @NotBlank(message = "condition ห้ามว่าง")
    private String condition;

    @Size(max = 500)
    private String remark;

    public ReturnRequestDto() {
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }
    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public String getCondition() {
        return condition;
    }
    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getRemark() {
        return remark;
    }
    public void setRemark(String remark) {
        this.remark = remark;
    }
}