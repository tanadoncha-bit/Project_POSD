package com.example.itborrow.service.avatar;

public class StorageException extends RuntimeException {
    public StorageException() {
        super("Image storage is unavailable. Please try again later or contact the administrator.");
    }
}
