package com.example.itborrow.service.impl;

import com.example.itborrow.service.*;

import com.example.itborrow.repository.AvatarRepository;
import com.example.itborrow.service.storage.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.util.UUID;

@Service
public class AvatarServiceImpl implements AvatarService {
    private final AvatarRepository avatars;
    private final ImageStorage storage;
    private final AvatarImageProcessor processor;
    private final TransactionTemplate transactions;
    private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AvatarService.class);

    public AvatarServiceImpl(AvatarRepository avatars, ImageStorage storage, AvatarImageProcessor processor,
            PlatformTransactionManager manager) {
        this.avatars = avatars;
        this.storage = storage;
        this.processor = processor;
        this.transactions = new TransactionTemplate(manager);
    }

    private record CachedUrl(String value, long expiresAt) {
    }

    private final java.util.concurrent.ConcurrentHashMap<String, CachedUrl> urlCache = new java.util.concurrent.ConcurrentHashMap<>();

    private String cachedUrl(String path) {
        if (urlCache.size() >= 512 && !urlCache.containsKey(path))
            urlCache.clear();
        return urlCache.compute(path, (key, entry) -> {
            long now = System.currentTimeMillis();
            if (entry != null && entry.expiresAt() > now)
                return entry;
            try {
                return new CachedUrl(storage.readUrl(key), now + 50 * 60 * 1000L);
            } catch (StorageException ex) {
                return new CachedUrl(null, now + 10 * 1000L);
            }
        }).value();
    }

    public String url(Long id) {
        String path = avatars.path(id);
        if (path != null) {
            try {
                return cachedUrl(path);
            } catch (StorageException ex) {
                return null;
            }
        }
        return null;
    }

    public String storedUrl(Long id) {
        String path = avatars.path(id);
        return path == null ? null : storage.readUrl(path);
    }

    public void save(Long id, MultipartFile file) {
        persist(id, processor.process(file));
    }

    private void persist(Long id, byte[] bytes) {
        String path = id + "/" + UUID.randomUUID() + ".png";
        storage.upload(path, bytes);
        String previous;
        try {
            previous = transactions.execute(tx -> {
                avatars.lock(id);
                String old = avatars.path(id);
                avatars.setPath(id, path);
                return old;
            });
        } catch (RuntimeException ex) {
            cleanup(path);
            throw ex;
        }
        if (previous != null)
            cleanup(previous);
    }

    private void cleanup(String path) {
        try {
            storage.delete(path);
        } catch (RuntimeException ex) {
            log.warn("Avatar object cleanup failed; reconcile unreferenced objects in Storage.");
        }
    }
}
