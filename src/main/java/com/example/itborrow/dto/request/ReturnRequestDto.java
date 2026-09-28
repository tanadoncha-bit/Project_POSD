package com.example.itborrow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class ReturnRequestDto {

    private LocalDate returnDate;
    @Size(max = 100)
    private java.util.List<@NotNull @jakarta.validation.Valid Inspection> items;
    public record Inspection(@NotNull Long equipmentId, @NotBlank String condition, @Size(max = 500) String remark) {}
    public java.util.List<Inspection> getItems() { return items; }
    public void setItems(java.util.List<Inspection> items) { this.items = items; }


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