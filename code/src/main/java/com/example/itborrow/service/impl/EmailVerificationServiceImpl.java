package com.example.itborrow.service.impl;

import com.example.itborrow.repository.EmailVerificationRepository;
import com.example.itborrow.service.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.*;
import java.time.Clock;
import java.util.*;

@Service
public class EmailVerificationServiceImpl implements EmailVerificationService {
    private final CurrentUser current;
    private final EmailVerificationRepository verifications;
    private final Clock clock;
    private final PersistentJobs jobs;
    private final String baseUrl;

    public EmailVerificationServiceImpl(
            CurrentUser current,
            EmailVerificationRepository verifications,
            Clock clock,
            PersistentJobs jobs,
            @Value("${app.public-base-url}") String baseUrl) {
        this.current = current;
        this.verifications = verifications;
        this.clock = clock;
        this.jobs = jobs;
        var uri = URI.create(baseUrl);
        if (uri.getHost() == null
                || !("https".equals(uri.getScheme())
                        || ("http".equals(uri.getScheme())
                                && ("localhost".equals(uri.getHost())
                                        || "127.0.0.1".equals(uri.getHost())))))
            throw new IllegalArgumentException(
                    "APP_BASE_URL must be an HTTPS application URL or local development URL.");
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    private String hash(String token) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Boolean> status() {
        var u = current.require();
        return Map.of(
                "configured",
                jobs.mailConfigured(),
                "verified",
                verifications.verified(u.getId(), u.getEmail()));
    }

    @Transactional
    public void request() {
        var u = current.require();
        if (!jobs.mailConfigured())
            throw new IllegalArgumentException(
                    "Email delivery is not configured. Contact the administrator.");
        verifications.lockUser(u.getId());
        if (verifications.verified(u.getId(), u.getEmail())) return;
        if (verifications.recentlyRequested(u.getId(), clock.instant().minusSeconds(60)))
            throw new IllegalArgumentException(
                    "Wait one minute before requesting another verification email.");
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        verifications.issue(
                u.getId(),
                u.getEmail(),
                hash(token),
                clock.instant().plusSeconds(1800),
                clock.instant());
        jobs.email(
                u.getEmail(),
                "Verify your LeadIT email",
                "Click the link below to verify your LeadIT email. The link expires in 30 minutes."
                        + " If you did not request this, ignore this email.\n\n"
                        + baseUrl
                        + "/verify-email?token="
                        + token);
    }

    @Transactional
    public void confirm(String token) {
        confirmToken(token, current.require().getId());
    }

    @Transactional(readOnly = true)
    public boolean validLink(String token) {
        return token != null
                && !token.isBlank()
                && token.length() <= 100
                && verifications.valid(hash(token));
    }

    @Transactional
    public void confirmLink(String token) {
        confirmToken(token, null);
    }

    private void confirmToken(String token, Long userId) {
        if (token == null
                || token.isBlank()
                || token.length() > 100
                || !verifications.confirm(hash(token), userId))
            throw new IllegalArgumentException("The verification link is invalid or expired.");
    }
}
