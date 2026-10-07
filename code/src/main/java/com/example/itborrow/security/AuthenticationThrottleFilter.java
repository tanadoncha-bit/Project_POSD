package com.example.itborrow.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

public class AuthenticationThrottleFilter extends OncePerRequestFilter {
    private final AuthenticationThrottle throttle;
    public AuthenticationThrottleFilter(AuthenticationThrottle throttle) { this.throttle = throttle; }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path.isEmpty()) path = request.getRequestURI().substring(request.getContextPath().length());
        return !"POST".equals(request.getMethod()) || !java.util.Set.of("/login", "/register", "/api/v1/users").contains(path);
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean registration = !request.getRequestURI().endsWith("/login");
        long retry = throttle.check(request.getRemoteAddr(), request.getParameter("username"), registration);
        if (retry > 0) {
            response.setStatus(429);
            response.setHeader("Retry-After", Long.toString(retry));
            if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":429,\"message\":\"Too many attempts. Please try again later.\"}");
            } else {
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write("<!doctype html><html lang='en'><head><meta name='viewport' content='width=device-width,initial-scale=1'><title>Please try again | LeadIT</title><link rel='stylesheet' href='/css/style.css'></head><body><main class='password-settings-page'><section class='password-settings-card'><h1>Please try again later</h1><p>Too many attempts. Wait a few minutes before signing in or registering again.</p><a href='/' class='password-settings-cancel'>Back to LeadIT</a></section></main></body></html>");
            }
            return;
        }
        chain.doFilter(request, response);
    }
}
