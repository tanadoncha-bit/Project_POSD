package com.example.itborrow.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.*;
import java.util.*;

@Service
public class EmailVerificationService {
    private final CurrentUser current;
    private final JdbcTemplate jdbc;
    private final PersistentJobs jobs;
    private final String baseUrl;

    public EmailVerificationService(CurrentUser current, JdbcTemplate jdbc, PersistentJobs jobs, @org.springframework.beans.factory.annotation.Value("${app.public-base-url}") String baseUrl) {
        this.current = current;
        this.jdbc = jdbc;
        this.jobs = jobs;
        var uri=java.net.URI.create(baseUrl);
        if(uri.getHost()==null || !("https".equals(uri.getScheme()) || ("http".equals(uri.getScheme()) && ("localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost()))))) throw new IllegalArgumentException("APP_BASE_URL must be an HTTPS application URL or local development URL.");
        this.baseUrl=baseUrl.replaceAll("/+$", "");
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Boolean> status() {
        var u = current.require();
        return Map.of("configured", jobs.mailConfigured(), "verified", jdbc.queryForObject(
                "SELECT count(*) FROM email_verifications WHERE user_id=? AND email=? AND verified_at IS NOT NULL",
                Long.class, u.getId(), u.getEmail()) > 0);
    }

    @Transactional
    public void request() {
        var u = current.require();
        if (!jobs.mailConfigured())
            throw new IllegalArgumentException("Email delivery is not configured. Contact the administrator.");
        jdbc.queryForObject("SELECT id FROM users WHERE id=? FOR UPDATE", Long.class, u.getId());
        if (jdbc.queryForObject("SELECT count(*) FROM email_verifications WHERE user_id=? AND requested_at>?",
                Long.class, u.getId(), java.sql.Timestamp.from(Instant.now().minusSeconds(60))) > 0)
            throw new IllegalArgumentException("Wait one minute before requesting another verification email.");
        byte[] bytes = new byte[24];
        new java.security.SecureRandom().nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.update("DELETE FROM email_verifications WHERE user_id=?", u.getId());
        jdbc.update(
                "INSERT INTO email_verifications(user_id,email,token_hash,expires_at,requested_at) VALUES (?,?,?,?,CURRENT_TIMESTAMP)",
                u.getId(), u.getEmail(), hash(token), java.sql.Timestamp.from(Instant.now().plusSeconds(1800)));
        jobs.email(u.getEmail(), "Verify your LeadIT email",
                "Click the link below to verify your LeadIT email. The link expires in 30 minutes. If you did not request this, ignore this email.\n\n" + baseUrl + "/verify-email?token=" + token);
    }

    @Transactional
    public void confirm(String token) {
        var u = current.require();
        if (token == null || token.length() > 100)
            throw new IllegalArgumentException("Invalid verification code.");
        int changed = jdbc.update(
                "UPDATE email_verifications SET verified_at=CURRENT_TIMESTAMP,token_hash=NULL WHERE user_id=? AND email=? AND token_hash=? AND expires_at>CURRENT_TIMESTAMP AND verified_at IS NULL",
                u.getId(), u.getEmail(), hash(token.trim()));
        if (changed != 1)
            throw new IllegalArgumentException("The verification code is invalid or expired.");
    }    @Transactional(readOnly=true)
    public boolean validLink(String token) {
        if(token==null || token.isBlank() || token.length()>100) return false;
        return jdbc.queryForObject("SELECT count(*) FROM email_verifications v JOIN users u ON u.id=v.user_id AND u.email=v.email WHERE v.token_hash=? AND v.expires_at>CURRENT_TIMESTAMP AND v.verified_at IS NULL",Long.class,hash(token))==1;
    }
    @Transactional
    public void confirmLink(String token) {
        if(token==null || token.isBlank() || token.length()>100) throw new IllegalArgumentException("The verification link is invalid or expired.");
        int changed=jdbc.update("UPDATE email_verifications SET verified_at=CURRENT_TIMESTAMP,token_hash=NULL WHERE token_hash=? AND expires_at>CURRENT_TIMESTAMP AND verified_at IS NULL AND email=(SELECT email FROM users WHERE users.id=email_verifications.user_id)",hash(token));
        if(changed!=1) throw new IllegalArgumentException("The verification link is invalid or expired.");
    }

}
