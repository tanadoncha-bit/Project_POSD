package com.example.itborrow.config;

import com.example.itborrow.repository.AvatarRepository;
import com.example.itborrow.service.AvatarService;
import org.springframework.stereotype.Component;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@ConditionalOnProperty(name="app.storage.migrate-legacy",havingValue="true")
public class LegacyAvatarMigration implements ApplicationRunner {
    private final AvatarRepository avatars; private final AvatarService service;
    public LegacyAvatarMigration(AvatarRepository avatars,AvatarService service) {this.avatars=avatars;this.service=service;}
    public void run(ApplicationArguments args) {
        for(Long id:avatars.legacyIds()) service.migrateLegacy(id);
    }
}
