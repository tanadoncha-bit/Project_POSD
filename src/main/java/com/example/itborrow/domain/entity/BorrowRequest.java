package com.example.itborrow.domain.entity;

import com.example.itborrow.domain.enums.BorrowStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "borrow_requests", indexes = {
        @Index(name = "idx_borrow_request_status", columnList = "status"),
        @Index(name = "idx_borrow_request_due_date", columnList = "due_date")
})
public class BorrowRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "borrow_date", nullable = false)
    private LocalDate borrowDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BorrowStatus status = BorrowStatus.PENDING;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "borrowRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BorrowItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "borrowRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private ReturnRecord returnRecord;

    public BorrowRequest() {
    }

    public BorrowRequest(
            Long id,
            User user,
            LocalDate borrowDate,
            LocalDate dueDate,
            BorrowStatus status,
            String note,
            LocalDateTime createdAt,
            List<BorrowItem> items,
            ReturnRecord returnRecord
    ) {
        this.id = id;
        this.user = user;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.status = status;
        this.note = note;
        this.createdAt = createdAt;
        this.items = items;
        this.returnRecord = returnRecord;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public LocalDate getBorrowDate() {
        return borrowDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public BorrowStatus getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<BorrowItem> getItems() {
        return items;
    }

    public ReturnRecord getReturnRecord() {
        return returnRecord;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setBorrowDate(LocalDate borrowDate) {
        this.borrowDate = borrowDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public void setStatus(BorrowStatus status) {
        this.status = status;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setItems(List<BorrowItem> items) {
        this.items = items;
    }

    public void setReturnRecord(ReturnRecord returnRecord) {
        this.returnRecord = returnRecord;
    }

    public void addItem(BorrowItem item) {
        items.add(item);
        item.setBorrowRequest(this);
    }
}
