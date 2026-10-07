package com.example.itborrow.security;

import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class AccountPrincipal extends org.springframework.security.core.userdetails.User {
    private final long securityVersion;

    public AccountPrincipal(com.example.itborrow.domain.entity.User account) {
        super(account.getUsername(), account.getPassword(), account.isLocalPasswordEnabled(), true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name())));
        securityVersion = account.getSecurityVersion();
    }

    public long getSecurityVersion() {
        return securityVersion;
    }
}
