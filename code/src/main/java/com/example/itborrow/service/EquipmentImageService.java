package com.example.itborrow.service;

import com.example.itborrow.service.storage.*;

import org.springframework.web.multipart.MultipartFile;

public interface EquipmentImageService {
    String save(Long id, MultipartFile file);

    String read(Long id, String filename);
}
