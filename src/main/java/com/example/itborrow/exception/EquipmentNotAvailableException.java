package com.example.itborrow.exception;

public class EquipmentNotAvailableException extends RuntimeException {
    public EquipmentNotAvailableException(String message) {
        super(message);
    }
}