package com.example.itborrow.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.*;
import java.util.*;
@Service
public class EmailVerificationService {
 private final CurrentUser current; private final JdbcTemplate jdbc; private final PersistentJobs jobs;
 public EmailVerificationService(CurrentUser current,JdbcTemplate jdbc,PersistentJobs jobs) { this.current=current;this.jdbc=jdbc;this.jobs=jobs; }
 private String hash(String token) { try { return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8))); } catch(java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); } }
 @Transactional(readOnly=true) public Map<String,Boolean> status() { var u=current.require(); return Map.of("configured",jobs.mailConfigured(),"verified",jdbc.queryForObject("SELECT count(*) FROM email_verifications WHERE user_id=? AND email=? AND verified_at IS NOT NULL",Long.class,u.getId(),u.getEmail())>0); }
 @Transactional public void request() {
  var u=current.require(); if(!jobs.mailConfigured()) throw new IllegalArgumentException("Email delivery is not configured. Contact the administrator.");
  jdbc.queryForObject("SELECT id FROM users WHERE id=? FOR UPDATE",Long.class,u.getId());
  if(jdbc.queryForObject("SELECT count(*) FROM email_verifications WHERE user_id=? AND requested_at>?",Long.class,u.getId(),java.sql.Timestamp.from(Instant.now().minusSeconds(60)))>0) throw new IllegalArgumentException("Wait one minute before requesting another code.");
  byte[] bytes=new byte[24]; new java.security.SecureRandom().nextBytes(bytes); String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  jdbc.update("DELETE FROM email_verifications WHERE user_id=?",u.getId());
  jdbc.update("INSERT INTO email_verifications(user_id,email,token_hash,expires_at,requested_at) VALUES (?,?,?,?,CURRENT_TIMESTAMP)",u.getId(),u.getEmail(),hash(token),java.sql.Timestamp.from(Instant.now().plusSeconds(1800)));
  jobs.email(u.getEmail(),"Verify your LeadIT email","Paste this code in your signed-in LeadIT profile within 30 minutes: "+token);
 }
 @Transactional public void confirm(String token) {
  var u=current.require(); if(token==null || token.length()>100) throw new IllegalArgumentException("Invalid verification code.");
  int changed=jdbc.update("UPDATE email_verifications SET verified_at=CURRENT_TIMESTAMP,token_hash=NULL WHERE user_id=? AND email=? AND token_hash=? AND expires_at>CURRENT_TIMESTAMP AND verified_at IS NULL",u.getId(),u.getEmail(),hash(token.trim()));
  if(changed!=1) throw new IllegalArgumentException("The verification code is invalid or expired.");
 }
}
