package com.example.itborrow.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Immutable receipt values: later equipment price changes do not change this
 * charge.
 */
@Embeddable
public class ReturnInspection {
    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;
    @Column(name = "equipment_name", nullable = false, length = 150)
    private String equipmentName;
    @Column(name = "condition", nullable = false, length = 50)
    private String condition;
    @Column(name = "purchase_price", precision = 12, scale = 2)
    private BigDecimal purchasePrice;
    @Column(name = "damage_rate", nullable = false, precision = 3, scale = 2)
    private BigDecimal damageRate;
    @Column(name = "damage_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal damageAmount;
    @Column(name = "remark", length = 500)
    private String remark;

    protected ReturnInspection() {
    }

    public ReturnInspection(Long id, String name, String condition, BigDecimal price, BigDecimal rate,
            BigDecimal amount, String remark) {
        this.equipmentId = id;
        this.equipmentName = name;
        this.condition = condition;
        this.purchasePrice = price;
        this.damageRate = rate;
        this.damageAmount = amount;
        this.remark = remark;
    }

    public Long getEquipmentId() {
        return equipmentId;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public String getCondition() {
        return condition;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public BigDecimal getDamageRate() {
        return damageRate;
    }

    public BigDecimal getDamageAmount() {
        return damageAmount;
    }

    public String getRemark() {
        return remark;
    }
}
