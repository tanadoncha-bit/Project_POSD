package com.example.itborrow.service.avatar;

public interface ImageStorage {
    void upload(String path, byte[] png);

    String readUrl(String path);

    void delete(String path);
}
