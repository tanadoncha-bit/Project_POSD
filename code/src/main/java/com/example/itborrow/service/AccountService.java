package com.example.itborrow.service;

import com.example.itborrow.domain.entity.*;
import com.example.itborrow.domain.enums.Role;
import com.example.itborrow.dto.request.*;
import com.example.itborrow.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final PasswordEncoder encoder;
    private final CurrentUser current;

    public AccountService(UserRepository users, UserProfileRepository profiles, PasswordEncoder encoder,
            CurrentUser current) {
        this.users = users;
        this.profiles = profiles;
        this.encoder = encoder;
        this.current = current;
    }

    private void validatePassword(String password, String confirm) {
        if (!password.equals(confirm))
            throw new IllegalArgumentException("Passwords do not match.");
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("Password must not exceed 72 UTF-8 bytes.");
    }

    @Transactional
    public User register(RegistrationDto dto) {
        validatePassword(dto.password(), dto.confirmPassword());
        if (users.existsByUsername(dto.username()) || users.existsByEmail(dto.email()))
            throw new IllegalArgumentException("Username or email already exists.");
        var user = new User(dto.username(), dto.email(), Role.USER);
        user.setPassword(encoder.encode(dto.password()));
        users.save(user);
        var profile = new UserProfile(dto.fullName(), dto.phone(), dto.department());
        profile.setUser(user);
        profiles.save(profile);
        return user;
    }

    @Transactional
    public void update(ProfileUpdateDto dto) {
        var user = current.require();
        if (users.existsByEmailAndIdNot(dto.email(), user.getId()))
            throw new IllegalArgumentException("Email already exists.");
        user.setEmail(dto.email());
        users.save(user);
        var profile = profiles.findByUserId(user.getId()).orElseGet(UserProfile::new);
        profile.setUser(user);
        profile.setFullName(dto.fullName());
        profile.setPhone(dto.phone());
        profile.setDepartment(dto.department());
        profiles.save(profile);
    }

    @Transactional
    public void changePassword(PasswordChangeDto dto) {
        var user = current.require();
        if (!encoder.matches(dto.currentPassword(), user.getPassword()))
            throw new IllegalArgumentException("Current password is incorrect.");
        validatePassword(dto.password(), dto.confirmPassword());
        user.setPassword(encoder.encode(dto.password()));
        users.save(user);
    }
}
