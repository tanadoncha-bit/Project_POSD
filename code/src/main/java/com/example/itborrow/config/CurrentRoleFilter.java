package com.example.itborrow.config;

import com.example.itborrow.repository.UserRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.filter.OncePerRequestFilter;

public class CurrentRoleFilter extends OncePerRequestFilter {
    private final UserRepository users;

    public CurrentRoleFilter(UserRepository users) {
        this.users = users;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return ("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod())) &&
                (path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/")
                        || path.equals("/favicon.ico") || path.equals("/favicon.svg"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            var account = users.findByUsername(auth.getName());
            if (account.isEmpty())
                SecurityContextHolder.clearContext();
            else {
                String path = request.getRequestURI().substring(request.getContextPath().length());
                if (!account.get().isLocalPasswordEnabled()
                        && !path.equals("/profile/setup-login") && !path.equals("/logout")
                        && !path.equals("/error") && !path.equals("/actuator/health")) {
                    if (path.startsWith("/api/")) {
                        response.setStatus(403);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"message\":\"Complete registration first.\",\"redirect\":\"/profile/setup-login\"}");
                    } else {
                        response.sendRedirect(request.getContextPath() + "/profile/setup-login");
                    }
                    return;
                }
                var updated = UsernamePasswordAuthenticationToken.authenticated(auth.getPrincipal(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + account.get().getRole().name())));
                updated.setDetails(auth.getDetails());
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(updated);
                SecurityContextHolder.setContext(context);
            }
        }
        chain.doFilter(request, response);
    }
}
