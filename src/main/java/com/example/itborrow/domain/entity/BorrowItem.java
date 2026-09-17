package com.example.itborrow.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "borrow_items")
public class BorrowItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "borrow_request_id", nullable = false)
    private BorrowRequest borrowRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(name = "quantity", nullable = false)
    private int quantity = 1;

    @Column(name = "condition_on_borrow", length = 200)
    private String conditionOnBorrow;


    public BorrowItem() {
    }

    public BorrowItem(Long id, BorrowRequest borrowRequest, Equipment equipment,
                      int quantity, String conditionOnBorrow) {
        this.id = id;
        this.borrowRequest = borrowRequest;
        this.equipment = equipment;
        this.quantity = quantity;
        this.conditionOnBorrow = conditionOnBorrow;
    }


    // Getters
    public Long getId() {
        return id;
    }

    public BorrowRequest getBorrowRequest() {
        return borrowRequest;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getConditionOnBorrow() {
        return conditionOnBorrow;
    }


    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setBorrowRequest(BorrowRequest borrowRequest) {
        this.borrowRequest = borrowRequest;
    }

    public void setEquipment(Equipment equipment) {
        this.equipment = equipment;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setConditionOnBorrow(String conditionOnBorrow) {
        this.conditionOnBorrow = conditionOnBorrow;
    }
}