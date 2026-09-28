package com.example.itborrow.service.avatar;

/** Port for image persistence; callers do not know provider credentials or HTTP APIs. */
public interface ImageStorage {
    void upload(String path, byte[] png);
    String readUrl(String path);
    void delete(String path);
}
