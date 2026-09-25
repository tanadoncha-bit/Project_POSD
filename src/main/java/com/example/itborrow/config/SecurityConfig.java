package com.example.itborrow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // หน้าที่เปิดดูได้โดยไม่ต้อง Login
                        .requestMatchers(
                                "/",
                                "/Dashboard",
                                "/equipment/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico",
                                "/error",
                                "/register"
                        ).permitAll()

                        // Swagger และ Health Check
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/health"
                        ).permitAll()

                        // สมัครสมาชิก
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/users"
                        ).permitAll()

                        // ทุกคนดูข้อมูลอุปกรณ์ได้
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/equipment/**"
                        ).permitAll()

                        // ADMIN และ MANAGER เพิ่มอุปกรณ์ได้
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/equipment/**"
                        ).hasAnyRole("ADMIN", "MANAGER")

                        // ADMIN และ MANAGER แก้ไขอุปกรณ์ได้
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/v1/equipment/**"
                        ).hasAnyRole("ADMIN", "MANAGER")

                        // เฉพาะ ADMIN ลบอุปกรณ์ได้
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/equipment/**"
                        ).hasRole("ADMIN")

                        // ต้อง Login ก่อนจึงใช้งานได้
                        .requestMatchers(
                                "/borrow",
                                "/my-requests",
                                "/profile",
                                "/api/v1/borrow-requests/**"
                        ).authenticated()

                        // ADMIN และ MANAGER เข้า Management ได้
                        .requestMatchers("/admin/**")
                        .hasAnyRole("ADMIN", "MANAGER")

                        // API จัดการผู้ใช้สำหรับ ADMIN
                        .requestMatchers("/api/v1/users/**")
                        .hasRole("ADMIN")

                        // URL อื่นต้อง Login
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/?loginError=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }

    /*
     * บัญชีชั่วคราวสำหรับทดสอบก่อนเชื่อมผู้ใช้จากฐานข้อมูล
     */
    @Bean
    public UserDetailsService userDetailsService(
            PasswordEncoder passwordEncoder
    ) {
        var user = User.withUsername("user")
                .password(passwordEncoder.encode("user123"))
                .roles("USER")
                .build();

        var manager = User.withUsername("manager")
                .password(passwordEncoder.encode("manager123"))
                .roles("MANAGER")
                .build();

        var admin = User.withUsername("admin")
                .password(passwordEncoder.encode("admin123"))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(
                user,
                manager,
                admin
        );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}