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
    @Bean public SecurityFilterChain securityFilterChain(HttpSecurity http, UserRepository users) throws Exception {
        http.addFilterBefore(new CurrentRoleFilter(users), org.springframework.security.web.access.intercept.AuthorizationFilter.class);
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/Dashboard", "/equipment/**", "/css/**", "/js/**", "/images/**", "/favicon.ico", "/error", "/register", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/actuator/health").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/equipment/**", "/api/v1/categories").permitAll()
            .requestMatchers("/admin/users", "/admin/users/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/api/v1/equipment/**").hasRole("ADMIN")
            .requestMatchers("/api/v1/equipment/**", "/admin/**").hasAnyRole("ADMIN", "STAFF")
            .requestMatchers("/api/v1/borrow-requests/*/approve").hasAnyRole("ADMIN", "STAFF")
            .requestMatchers(HttpMethod.POST, "/api/v1/borrow-requests/*/return").hasAnyRole("ADMIN", "STAFF")
            .requestMatchers("/api/v1/users/**").hasRole("ADMIN")
            .anyRequest().authenticated())
        .formLogin(form -> form.loginPage("/").loginProcessingUrl("/login").defaultSuccessUrl("/", true).failureUrl("/?loginError=true").permitAll())
        .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/").invalidateHttpSession(true).deleteCookies("JSESSIONID"))
        .exceptionHandling(errors -> errors.defaultAuthenticationEntryPointFor(
            (request, response, exception) -> response.sendError(401),
            request -> request.getRequestURI().startsWith("/api/")));
        return http.build();
    }
    @Bean public UserDetailsService userDetailsService(UserRepository users) {
        return username -> {
            var user = users.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Account not found"));
            return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                .password(user.getPassword()).roles(user.getRole().name()).build();
        };
    }
    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
