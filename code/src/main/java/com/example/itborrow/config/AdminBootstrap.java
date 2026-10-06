package com.example.itborrow.config;

import com.example.itborrow.domain.entity.User;
import com.example.itborrow.domain.enums.Role;
import com.example.itborrow.repository.UserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final Environment env;
    private final PasswordEncoder encoder;

    public AdminBootstrap(JdbcTemplate jdbc, UserRepository users, Environment env, PasswordEncoder encoder) {
        this.jdbc = jdbc;
        this.users = users;
        this.env = env;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String username = env.getProperty("app.bootstrap-admin.username", "");
        if (username.isBlank())
            return;
        jdbc.queryForObject("SELECT id FROM role_management_lock WHERE id=1 FOR UPDATE", Long.class);
        if (users.countByRole(Role.ADMIN) > 0)
            return;
        String email = env.getProperty("app.bootstrap-admin.email", "");
        String password = env.getProperty("app.bootstrap-admin.password", "");
        if (!username.matches("[A-Za-z0-9_.-]{3,50}") || email.length() > 100
                || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") || password.length() < 12
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new IllegalStateException(
                    "Bootstrap requires a valid username/email and a password of at least 12 characters, at most 72 UTF-8 bytes.");
        if (users.existsByUsername(username) || users.existsByEmail(email))
            throw new IllegalStateException(
                    "Bootstrap account already exists. Choose a new dedicated administrator username/email.");
        var admin = new User(username, email, Role.ADMIN);
        admin.setPassword(encoder.encode(password));
        users.saveAndFlush(admin);
        jdbc.update(
                "INSERT INTO role_audit(actor_username,target_username,old_role,new_role) VALUES ('SYSTEM',?,NULL,'ADMIN')",
                username);
    }
}
