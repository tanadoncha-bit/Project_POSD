package com.example.itborrow.service.impl;

import com.example.itborrow.service.*;

import com.example.itborrow.dto.request.EmailChangeDto;
import com.example.itborrow.repository.*;
import java.time.Clock;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailChangeServiceImpl implements EmailChangeService {
    private final UserRepository users;
    private final EmailChangeRepository changes;
    private final EmailVerificationRepository verification;
    private final CurrentUser current;
    private final PasswordEncoder passwords;
    private final PersistentJobs jobs;
    private final Clock clock;
    private final String baseUrl;
    public EmailChangeServiceImpl(UserRepository users, EmailChangeRepository changes, EmailVerificationRepository verification,
            CurrentUser current, PasswordEncoder passwords, PersistentJobs jobs, Clock clock,
            @Value("${app.public-base-url}") String baseUrl) {
        this.users=users; this.changes=changes; this.verification=verification; this.current=current;
        this.passwords=passwords; this.jobs=jobs; this.clock=clock; this.baseUrl=baseUrl.replaceAll("/+$", "");
    }
    private String hash(String token) {
        try { return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
    @Transactional public void request(EmailChangeDto dto) {
        var user=users.findLockedById(current.require().getId()).orElseThrow();
        if (!user.isLocalPasswordEnabled() || !passwords.matches(dto.currentPassword(),user.getPassword()))
            throw new IllegalArgumentException("Current password is incorrect.");
        String email=dto.email().trim();
        if (user.getEmail().equalsIgnoreCase(email)) throw new IllegalArgumentException("Choose a different email address.");
        if (users.findByEmailIgnoreCase(email).isPresent()) throw new IllegalArgumentException("Email is already in use.");
        if (!jobs.mailConfigured()) throw new IllegalArgumentException("Email delivery is not configured.");
        if (changes.recent(user.getId(),clock.instant().minusSeconds(60))) throw new IllegalArgumentException("Wait one minute before trying again.");
        byte[] bytes=new byte[24];new java.security.SecureRandom().nextBytes(bytes);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        changes.issue(user.getId(),user.getEmail(),email,user.getSecurityVersion(),hash(token),clock.instant());
        jobs.email(email,"Confirm your new LeadIT email","Confirm this address to change your LeadIT email. The link expires in 30 minutes.\n\n"+baseUrl+"/confirm-email-change?token="+token);
        jobs.email(user.getEmail(),"LeadIT email change requested","An email change was requested for your LeadIT account. Your current email remains unchanged until the new address is confirmed. If this was not you, change your password to cancel this request and sign out other sessions.");
    }
    @Transactional(readOnly=true) public boolean valid(String token) {
        if (token==null || token.isBlank() || token.length()>100) return false;
        return changes.find(hash(token)).map(row -> users.findById(((Number)row.get("user_id")).longValue())
                .map(user -> matches(user,row)).orElse(false)).orElse(false);
    }
    private boolean matches(com.example.itborrow.domain.entity.User user, Map<String,Object> row) {
        return user.getEmail().equals(row.get("old_email")) && user.getSecurityVersion()==((Number)row.get("security_version")).longValue();
    }
    @Transactional public void confirm(String token) {
        if (token==null || token.isBlank() || token.length()>100) throw new IllegalArgumentException("Link is invalid or expired.");
        String hash=hash(token);
        var initial=changes.find(hash).orElseThrow(() -> new IllegalArgumentException("Link is invalid or expired."));
        long id=((Number)initial.get("user_id")).longValue();
        var user=users.findLockedById(id).orElseThrow();
        var row=changes.lock(id,hash).orElseThrow(() -> new IllegalArgumentException("Link is invalid or expired."));
        if (!matches(user,row)) throw new IllegalArgumentException("Link is invalid or expired.");
        String email=(String)row.get("new_email");
        if (users.findByEmailIgnoreCase(email).filter(other -> !other.getId().equals(id)).isPresent()) throw new IllegalArgumentException("Email is already in use.");
        user.setEmail(email);user.revokeSessions();users.saveAndFlush(user);
        verification.markVerified(id,email);changes.consume(id);
    }
}
