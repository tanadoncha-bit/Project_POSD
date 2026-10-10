package com.example.itborrow.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public class ReturnRequestDto {

    private boolean partial;

    public boolean isPartial() {
        return partial;
    }

    public void setPartial(boolean value) {
        partial = value;
    }

    private LocalDate returnDate;

    @Size(max = 100)
    private List<@NotNull @Valid Inspection> items;

    public record Inspection(
            @NotNull Long equipmentId,
            @NotBlank String condition,
            @Size(max = 500) String remark) {}

    public List<Inspection> getItems() {
        return items;
    }

    public void setItems(List<Inspection> items) {
        this.items = items;
    }

    private String condition;

    @Size(max = 500)
    private String remark;

    public ReturnRequestDto() {}

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
