package com.example.itborrow.config;

import com.example.itborrow.repository.UserRepository;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UserRepository users, org.springframework.beans.factory.ObjectProvider<org.springframework.security.oauth2.client.registration.ClientRegistrationRepository> registrations, com.example.itborrow.security.GoogleOidcUserService googleUsers) throws Exception {
        http.addFilterBefore(new CurrentRoleFilter(users),
                org.springframework.security.web.access.intercept.AuthorizationFilter.class);
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/Dashboard", "/equipment/**", "/css/**", "/js/**", "/images/**", "/favicon.ico",
                        "/error", "/register", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                        "/actuator/health", "/api/v1/auth/providers", "/verify-email", "/oauth2/**", "/login/oauth2/**")
                .permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/equipment/**", "/api/v1/categories", "/api/v1/categories/*")
                .permitAll()
                .requestMatchers("/admin/users", "/admin/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/equipment/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/equipment/**", "/admin/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers("/api/v1/borrow-requests/*/approve").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(HttpMethod.POST, "/api/v1/borrow-requests/*/return").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers("/api/v1/categories/**", "/api/v1/users/**").hasRole("ADMIN")
                .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/").loginProcessingUrl("/login").defaultSuccessUrl("/", true)
                        .failureUrl("/?loginError=true").permitAll())
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/").invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))
                .exceptionHandling(errors -> errors.defaultAuthenticationEntryPointFor(
                        (request, response, exception) -> response.sendError(401),
                        request -> request.getRequestURI().startsWith("/api/")));
        if(registrations.getIfAvailable()!=null) {
            http.oauth2Login(oauth -> oauth.loginPage("/").userInfoEndpoint(info -> info.oidcUserService(googleUsers))
                .successHandler((request, response, authentication) -> {
                    var account = users.findByUsername(authentication.getName()).orElseThrow();
                    String destination = account.isLocalPasswordEnabled() ? "/profile" : "/profile/setup-login";
                    response.sendRedirect(request.getContextPath() + destination);
                })
                .failureHandler((request,response,error) -> {
                    String message="Unable to sign in with Google. Please try again.";
                    if(error instanceof org.springframework.security.oauth2.core.OAuth2AuthenticationException oauthError && "account_exists".equals(oauthError.getError().getErrorCode()))
                        message="Sign in to your existing account and Verify Email first. Then use Continue with Google.";
                    request.getSession().setAttribute("googleLoginMessage",message);
                    response.sendRedirect(request.getContextPath()+"/?googleLoginError=true");
                }));
        }
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository users) {
        return username -> {
            var user = users.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
            return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                    .password(user.getPassword()).roles(user.getRole().name()).disabled(!user.isLocalPasswordEnabled()).build();
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
