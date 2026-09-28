package com.example.itborrow.domain.enums;

import java.math.BigDecimal;
import java.util.Locale;

public enum ReturnCondition {
    NORMAL("0.00", EquipmentStatus.AVAILABLE),
    MINOR_SCRATCHES("0.20", EquipmentStatus.AVAILABLE),
    DAMAGED("0.50", EquipmentStatus.MAINTENANCE),
    LOST("1.00", EquipmentStatus.DISPOSED);

    private final BigDecimal rate;
    private final EquipmentStatus status;
    ReturnCondition(String rate, EquipmentStatus status) { this.rate = new BigDecimal(rate); this.status = status; }
    public BigDecimal getRate() { return rate; }
    public EquipmentStatus getStatus() { return status; }
    public static ReturnCondition parse(String value) {
        String name = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        return switch (name) {
            case "GOOD", "NORMAL" -> NORMAL;
            case "SCRATCH", "MINOR_SCRATCHES" -> MINOR_SCRATCHES;
            case "DAMAGE", "DAMAGED" -> DAMAGED;
            case "LOST" -> LOST;
            default -> throw new IllegalArgumentException("Invalid return condition.");
        };
    }
}
