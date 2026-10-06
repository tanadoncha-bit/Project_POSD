package com.example.itborrow.service.impl;

import com.example.itborrow.repository.*;
import com.example.itborrow.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class AccountViewQueryImpl implements AccountViewQuery {
    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final AvatarService avatars;

    public AccountViewQueryImpl(UserRepository users, UserProfileRepository profiles, AvatarService avatars) {
        this.users = users;
        this.profiles = profiles;
        this.avatars = avatars;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> account(String username, boolean includeProfile) {
        Map<String, Object> result = new HashMap<>();
        users.findByUsername(username).ifPresent(user -> {
            result.put("user", user);
            result.put("currentUser", user);
            result.put("profileImageUrl", avatars.url(user.getId()));
            if (includeProfile) {
                var profile = profiles.findByUserId(user.getId());
                result.put("profileDisplayName", profile.map(p -> p.getFullName())
                        .filter(name -> name != null && !name.isBlank()).orElse(user.getUsername()));
                profile.ifPresent(value -> result.put("profile", value));
            }

        });
        return result;
    }
}
