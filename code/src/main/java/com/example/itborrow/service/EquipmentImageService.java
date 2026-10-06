package com.example.itborrow.service;

import com.example.itborrow.repository.EquipmentRepository;
import com.example.itborrow.service.avatar.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class EquipmentImageService {
    private final PersistentJobs jobs;
    private final ImageStorage storage;
    private final EquipmentImageProcessor processor;
    private final EquipmentRepository equipment;
    private final com.example.itborrow.repository.BorrowItemRepository borrowItems;
    private final CurrentUser current;
    private final TransactionTemplate transactions;

    public EquipmentImageService(EquipmentImageProcessor processor, EquipmentRepository equipment, CurrentUser current,
            PlatformTransactionManager manager,
            @org.springframework.beans.factory.annotation.Qualifier("equipmentStorage") ImageStorage storage,
            com.example.itborrow.repository.BorrowItemRepository borrowItems, PersistentJobs jobs) {
        this.jobs = jobs;
        this.borrowItems = borrowItems;
        this.storage = storage;
        this.processor = processor;
        this.equipment = equipment;
        this.current = current;
        this.transactions = new TransactionTemplate(manager);
    }

    public String save(Long id, MultipartFile file) {
        current.requireOperator();
        if (!equipment.existsById(id))
            throw new IllegalArgumentException("Equipment not found.");
        byte[] bytes = processor.process(file);
        String path = id + "/" + java.util.UUID.randomUUID() + ".png";
        String imageUrl = "/images/equipment/" + path;
        storage.upload(path, bytes);
        String old;
        try {
            old = transactions.execute(tx -> {
                var item = equipment.findLockedById(id).orElseThrow();
                String previous = item.getImageUrl();
                item.setImageUrl(imageUrl);
                equipment.saveAndFlush(item);
                return previous;
            });
        } catch (RuntimeException ex) {
            cleanup(path);
            throw ex;
        }
        if (old != null && old.startsWith("/images/equipment/") && !borrowItems.existsBySnapshotImageUrl(old))
            cleanup(old.substring("/images/equipment/".length()));
        return imageUrl;
    }

    public String read(Long id, String filename) {
        String path = id + "/" + filename;
        var item = equipment.findById(id).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND));
        if (!("/images/equipment/" + path).equals(item.getImageUrl())
                && !borrowItems.existsBySnapshotImageUrl("/images/equipment/" + path))
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND);
        if (urls.size() >= 512 && !urls.containsKey(path))
            urls.clear();
        return urls.compute(path, (key, cached) -> {
            long now = System.currentTimeMillis();
            return cached != null && cached.expiresAt() > now ? cached
                    : new SignedUrl(storage.readUrl(key), now + 50 * 60 * 1000L);
        }).url();
    }

    private record SignedUrl(String url, long expiresAt) {
    }

    private final java.util.concurrent.ConcurrentHashMap<String, SignedUrl> urls = new java.util.concurrent.ConcurrentHashMap<>();

    @org.springframework.transaction.event.TransactionalEventListener(phase = org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT)
    public void equipmentDeleted(com.example.itborrow.common.event.EquipmentDeleted event) {
        if (event.imageUrl() != null && event.imageUrl().startsWith("/images/equipment/"))
            cleanup(event.imageUrl().substring("/images/equipment/".length()));
    }

    private void cleanup(String path) {
        urls.remove(path);
        try {
            storage.delete(path);
        } catch (RuntimeException ex) {
            jobs.cleanup(path);
        }
    }
}
