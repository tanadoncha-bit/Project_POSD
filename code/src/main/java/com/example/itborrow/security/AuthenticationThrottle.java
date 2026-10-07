package com.example.itborrow.security;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationThrottle {
    private record Bucket(Instant expires, int count) {
    }

    private final Map<String, Bucket> buckets = new HashMap<>();
    private final Clock clock;
    private final int loginLimit, registrationLimit;

    public AuthenticationThrottle(Clock clock, @Value("${app.security.login-limit:20}") int loginLimit,
            @Value("${app.security.registration-limit:10}") int registrationLimit) {
        if (loginLimit < 1 || registrationLimit < 1)
            throw new IllegalArgumentException("Invalid throttle limits");
        this.clock = clock;
        this.loginLimit = loginLimit;
        this.registrationLimit = registrationLimit;
    }

    public synchronized long check(String source, String username, boolean registration) {
        var now = clock.instant();
        buckets.entrySet().removeIf(entry -> !entry.getValue().expires().isAfter(now));
        long window = registration ? 3600 : 600;
        int limit = registration ? registrationLimit : loginLimit;
        List<String> keys = registration ? List.of("register:ip:" + source)
                : List.of("login:ip:" + source, "login:account:" + digest(username));
        long retry = 0;
        for (var key : keys) {
            var bucket = buckets.get(key);
            int keyLimit = key.startsWith("login:ip:") ? limit * 5 : limit;
            if (bucket != null && bucket.count() >= keyLimit)
                retry = Math.max(retry, Math.max(1, java.time.Duration.between(now, bucket.expires()).getSeconds()));
        }
        if (retry > 0)
            return retry;
        if (buckets.size() + keys.stream().filter(key -> !buckets.containsKey(key)).count() > 10000)
            return 60;
        for (var key : keys) {
            var old = buckets.get(key);
            buckets.put(key, new Bucket(old == null ? now.plusSeconds(window) : old.expires(),
                    old == null ? 1 : old.count() + 1));
        }
        return 0;
    }

    private String digest(String value) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest((value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT))
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }
}
