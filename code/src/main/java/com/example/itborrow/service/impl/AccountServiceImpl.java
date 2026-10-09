package com.example.itborrow.service.impl;

import com.example.itborrow.domain.entity.*;
import com.example.itborrow.domain.enums.Role;
import com.example.itborrow.dto.request.*;
import com.example.itborrow.repository.*;
import com.example.itborrow.security.PasswordPolicy;
import com.example.itborrow.service.*;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountServiceImpl implements AccountService {
    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final PasswordEncoder encoder;
    private final CurrentUser current;
    private final PasswordPolicy passwordPolicy;

    public AccountServiceImpl(
            UserRepository users,
            UserProfileRepository profiles,
            PasswordEncoder encoder,
            CurrentUser current,
            PasswordPolicy passwordPolicy) {
        this.users = users;
        this.profiles = profiles;
        this.encoder = encoder;
        this.current = current;
        this.passwordPolicy = passwordPolicy;
    }

    @Transactional
    public User register(RegistrationDto dto) {
        passwordPolicy.validate(dto.password(), dto.confirmPassword());
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
        if (!user.getEmail().equals(dto.email()))
            throw new IllegalArgumentException("Use Change email to verify a new address first.");
        users.save(user);
        var profile = profiles.findByUserId(user.getId()).orElseGet(UserProfile::new);
        profile.setUser(user);
        profile.setFullName(dto.fullName());
        profile.setPhone(dto.phone());
        profile.setDepartment(dto.department());
        profiles.save(profile);
    }

    @Transactional
    public User changePassword(PasswordChangeDto dto) {
        var user = users.findLockedById(current.require().getId()).orElseThrow();
        if (!user.isLocalPasswordEnabled())
            throw new IllegalArgumentException("Set up your username and password first.");
        if (!encoder.matches(dto.currentPassword(), user.getPassword()))
            throw new IllegalArgumentException("Current password is incorrect.");
        passwordPolicy.validate(dto.password(), dto.confirmPassword());
        user.setPassword(encoder.encode(dto.password()));
        user.revokeSessions();
        users.saveAndFlush(user);
        return user;
    }

    public boolean requiresLoginSetup() {
        return !current.require().isLocalPasswordEnabled();
    }

    @Transactional
    public User setupLogin(LoginSetupDto dto) {
        var account = users.findLockedById(current.require().getId()).orElseThrow();
        if (account.isLocalPasswordEnabled())
            throw new IllegalArgumentException("Username and password are already set.");
        passwordPolicy.validate(dto.password(), dto.confirmPassword());
        if (users.findByUsername(dto.username())
                .filter(user -> !user.getId().equals(account.getId()))
                .isPresent()) throw new IllegalArgumentException("Username is already taken.");
        account.setUsername(dto.username());
        account.setPassword(encoder.encode(dto.password()));
        account.setLocalPasswordEnabled(true);
        account.revokeSessions();
        users.saveAndFlush(account);
        var profile = profiles.findByUserId(account.getId()).orElseGet(UserProfile::new);
        profile.setUser(account);
        profile.setFullName(dto.fullName());
        profile.setPhone(dto.phone());
        profile.setDepartment(dto.department());
        profiles.save(profile);
        return account;
    }
}
