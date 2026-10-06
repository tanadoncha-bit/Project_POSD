package com.example.itborrow.exception;

public class InvalidBorrowStateException extends RuntimeException {
    public InvalidBorrowStateException(String message) {
        super(message);
    }
}