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


    @Column(name = "snapshot_name", length = 150, updatable = false)
    private String snapshotName;
    public String getSnapshotName() { return snapshotName; }
    public void setSnapshotName(String value) { snapshotName = value; }
    @Column(name = "snapshot_asset_code", length = 30, updatable = false)
    private String snapshotAssetCode;
    public String getSnapshotAssetCode() { return snapshotAssetCode; }
    public void setSnapshotAssetCode(String value) { snapshotAssetCode = value; }
    @Column(name = "snapshot_storage_slot", length = 100, updatable = false)
    private String snapshotStorageSlot;
    public String getSnapshotStorageSlot() { return snapshotStorageSlot; }
    public void setSnapshotStorageSlot(String value) { snapshotStorageSlot = value; }
    @Column(name = "snapshot_image_url", length = 1000, updatable = false)
    private String snapshotImageUrl;
    public String getSnapshotImageUrl() { return snapshotImageUrl; }
    public void setSnapshotImageUrl(String value) { snapshotImageUrl = value; }
    @Column(name = "snapshot_category_name", length = 150, updatable = false)
    private String snapshotCategoryName;
    public String getSnapshotCategoryName() { return snapshotCategoryName; }
    public void setSnapshotCategoryName(String value) { snapshotCategoryName = value; }
    @Column(name="snapshot_purchase_price", precision=12, scale=2, updatable=false)
    private java.math.BigDecimal snapshotPurchasePrice;
    public java.math.BigDecimal getSnapshotPurchasePrice() { return snapshotPurchasePrice; }
    public void setSnapshotPurchasePrice(java.math.BigDecimal value) { snapshotPurchasePrice=value; }
    @Column(name="returned_on")
    private java.time.LocalDate returnedOn;
    public java.time.LocalDate getReturnedOn() { return returnedOn; }
    public void setReturnedOn(java.time.LocalDate value) { returnedOn=value; }
    @PrePersist
    public void captureEquipmentIdentity() {
        if (snapshotName == null && equipment != null) {
            snapshotPurchasePrice = equipment.getPurchasePrice();
            snapshotName = equipment.getName();
            snapshotAssetCode = equipment.getAssetCode();
            snapshotStorageSlot = equipment.getStorageSlot();
            snapshotImageUrl = equipment.getImageUrl();
        }
    }

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