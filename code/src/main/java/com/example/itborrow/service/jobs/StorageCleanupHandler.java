package com.example.itborrow.service.jobs;

import com.example.itborrow.repository.EquipmentImageReferenceRepository;
import com.example.itborrow.service.storage.ImageStorage;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class StorageCleanupHandler implements DeliveryJobHandler {
    private final ImageStorage storage;
    private final EquipmentImageReferenceRepository references;

    public StorageCleanupHandler(
            @Qualifier("equipmentStorage") ImageStorage storage,
            EquipmentImageReferenceRepository references) {
        this.storage = storage;
        this.references = references;
    }

    public String kind() {
        return "STORAGE_DELETE";
    }

    public void execute(Map<String, Object> job) {
        String path = (String) job.get("payload");
        if (!references.isReferenced(path)) storage.delete(path);
    }
}
