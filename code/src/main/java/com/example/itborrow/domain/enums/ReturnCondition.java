package com.example.itborrow.domain.enums;

import java.util.Locale;

public enum ReturnCondition {
    NORMAL(EquipmentStatus.AVAILABLE),
    MINOR_SCRATCHES(EquipmentStatus.AVAILABLE),
    DAMAGED(EquipmentStatus.MAINTENANCE),
    LOST(EquipmentStatus.DISPOSED);

    private final EquipmentStatus status;
    ReturnCondition(EquipmentStatus status) { this.status = status; }
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
