package com.example.itborrow;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.itborrow.security.AccountPrincipal;
import com.example.itborrow.service.GoogleAccountService;
import com.example.itborrow.service.storage.ImageStorage;

import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:google_login;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.jpa.hibernate.ddl-auto=none",
            "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
            "app.oauth.google.enabled=true",
            "app.oauth.google.client-id=test-client",
            "app.oauth.google.client-secret=test-secret",
            "spring.flyway.enabled=false",
            "management.health.mail.enabled=false",
            "app.jobs.enabled=false",
            "app.mail.from=tests@example.test",
            "spring.mail.host=smtp.example.test",
            "spring.sql.init.mode=always",
            "spring.jpa.show-sql=false",
            "logging.level.org.hibernate.SQL=warn",
            "borrow.overdue.initial-delay-ms=86400000",
            "logging.level.root=WARN",
            "logging.level.org.springframework=WARN",
            "debug=false"
        })
@AutoConfigureMockMvc
class GoogleOAuthIntegrationTest {
    @MockitoBean ImageStorage images;
    @MockitoBean JavaMailSender mail;
    @Autowired MockMvc mvc;

    @Test
    void configuredGoogleProviderIsPubliclyDiscoverable() throws Exception {
        mvc.perform(get("/api/v1/auth/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.google").value(true))
                .andExpect(jsonPath("$.kku").value(false));
    }

    @Test
    void googleSignInRedirectsToProviderWithStateAndOidcScope() throws Exception {
        mvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        header().string(
                                        "Location",
                                        Matchers.allOf(
                                                Matchers.startsWith("https://accounts.google.com/"),
                                                Matchers.containsString("state="),
                                                Matchers.containsString("openid"),
                                                Matchers.containsString("test-client"))));
    }

    @Test
    void callbackWithoutSavedAuthorizationStateCannotLogIn() throws Exception {
        mvc.perform(
                        get("/login/oauth2/code/google")
                                .param("code", "invalid")
                                .param("state", "invalid"))
                .andExpect(redirectedUrl("/?googleLoginError=true"));
    }

    @Autowired GoogleAccountService accounts;

    @Test
    void incompleteAccountMustFinishSignupAndCannotChangeGoogleEmail() throws Exception {
        var account =
                accounts.signIn(
                        "signup-flow-sub", "signup-flow@example.test", true, "New Member", null);
        var auth = SecurityMockMvcRequestPostProcessors.user(account.getUsername());
        mvc.perform(get("/profile").with(auth)).andExpect(redirectedUrl("/profile/setup-login"));
        mvc.perform(get("/api/v1/auth/providers").with(auth)).andExpect(status().isForbidden());
        mvc.perform(get("/profile/setup-login").with(auth))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("signup-flow@example.test")))
                .andExpect(content().string(Matchers.containsString("readonly")));
        mvc.perform(
                        post("/profile/setup-login")
                                .with(auth)
                                .with(SecurityMockMvcRequestPostProcessors.csrf())
                                .param("username", "completed_google_user")
                                .param("password", "chosen-password")
                                .param("confirmPassword", "chosen-password")
                                .param("fullName", "New Member")
                                .param("email", "attacker@example.test"))
                .andExpect(redirectedUrl("/profile"));
        var saved =
                accounts.signIn(
                        "signup-flow-sub", "signup-flow@example.test", true, "New Member", null);
        Assertions.assertThat(saved.getUsername()).isEqualTo("completed_google_user");
        Assertions.assertThat(saved.getEmail()).isEqualTo("signup-flow@example.test");
        mvc.perform(
                        get("/profile")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.user(
                                                new AccountPrincipal(saved))))
                .andExpect(status().isOk());
    }
}
