package com.example.itborrow.dto.response;

import java.time.LocalDate;
import java.util.List;

public class BorrowResponseDto {

    private Long id;
    private Long userId;
    private String username;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private String status;
    private String note;
    private List<BorrowItemResponseDto> items;

    private BorrowResponseDto() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final BorrowResponseDto dto = new BorrowResponseDto();

        public Builder id(Long id) {
            dto.id = id; return this;
        }
        public Builder userId(Long userId) {
            dto.userId = userId; return this;
        }
        public Builder username(String username) {
            dto.username = username; return this;
        }
        public Builder borrowDate(LocalDate borrowDate) {
            dto.borrowDate = borrowDate; return this;
        }
        public Builder dueDate(LocalDate dueDate) {
            dto.dueDate = dueDate; return this;
        }
        public Builder status(String status) {
            dto.status = status; return this;
        }
        public Builder note(String note) {
            dto.note = note; return this;
        }
        public Builder items(List<BorrowItemResponseDto> items) {
            dto.items = items; return this;
        }

        public BorrowResponseDto build() {
            return dto;
        }
    }

    public Long getId() {
        return id;
    }
    public Long getUserId() {
        return userId;
    }
    public String getUsername() {
        return username;
    }
    public LocalDate getBorrowDate() {
        return borrowDate;
    }
    public LocalDate getDueDate() {
        return dueDate;
    }
    public String getStatus() {
        return status;
    }
    public String getNote() {
        return note;
    }
    public List<BorrowItemResponseDto> getItems() {
        return items;
    }
}