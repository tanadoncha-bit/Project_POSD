package com.example.itborrow.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReturnResponseDto {

    private Long id;
    private Long borrowRequestId;
    private LocalDate returnDate;
    private String condition;
    private BigDecimal fineAmount;
    private String remark;

    private ReturnResponseDto() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ReturnResponseDto dto = new ReturnResponseDto();

        public Builder id(Long id) {
            dto.id = id; return this;
        }
        public Builder borrowRequestId(Long borrowRequestId) {
            dto.borrowRequestId = borrowRequestId; return this;
        }
        public Builder returnDate(LocalDate returnDate) {
            dto.returnDate = returnDate; return this;
        }
        public Builder condition(String condition) {
            dto.condition = condition; return this;
        }
        public Builder fineAmount(BigDecimal fineAmount) {
            dto.fineAmount = fineAmount; return this;
        }
        public Builder remark(String remark) {
            dto.remark = remark; return this;
        }

        public ReturnResponseDto build() {
            return dto;
        }
    }

    public Long getId() {
        return id;
    }
    public Long getBorrowRequestId() {
        return borrowRequestId;
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
}