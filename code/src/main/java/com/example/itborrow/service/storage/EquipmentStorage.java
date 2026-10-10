package com.example.itborrow.service.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EquipmentStorage extends SupabaseImageStorage {
    public EquipmentStorage(
            @Value("${app.storage.url:}") String url,
            @Value("${app.storage.service-key:}") String key,
            @Value("${app.storage.equipment-bucket:equipment}") String bucket) {
        super(url, key, bucket);
    }
}
