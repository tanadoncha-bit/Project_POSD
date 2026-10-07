package com.example.itborrow.service;

import com.example.itborrow.repository.AvatarRepository;
import com.example.itborrow.service.storage.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.util.UUID;

/** Application contract; persistence and orchestration reside in its implementation. */
public interface AvatarService {
    String url(Long id);
    String storedUrl(Long id);
    void save(Long id, MultipartFile file);
}
