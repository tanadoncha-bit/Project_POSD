package com.example.itborrow.service;

import com.example.itborrow.repository.EquipmentRepository;
import com.example.itborrow.service.storage.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Application contract; persistence and orchestration reside in its implementation. */
public interface EquipmentImageService {
    String save(Long id, MultipartFile file);
    String read(Long id, String filename);
}
