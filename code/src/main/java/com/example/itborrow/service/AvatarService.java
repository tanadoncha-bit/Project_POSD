package com.example.itborrow.service;

import com.example.itborrow.service.storage.*;

import org.springframework.web.multipart.MultipartFile;

public interface AvatarService {
    String url(Long id);

    String storedUrl(Long id);

    void save(Long id, MultipartFile file);
}
