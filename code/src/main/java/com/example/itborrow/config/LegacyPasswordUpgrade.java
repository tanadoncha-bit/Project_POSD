package com.example.itborrow.config;

import com.example.itborrow.repository.UserRepository;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LegacyPasswordUpgrade implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public LegacyPasswordUpgrade(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (var user : users.findAll()) {
            String password = user.getPassword();
            if (password != null && !password.startsWith("$2") && !password.startsWith("{"))
                users.upgradePassword(user.getId(), password, encoder.encode(password));
        }
    }
}
