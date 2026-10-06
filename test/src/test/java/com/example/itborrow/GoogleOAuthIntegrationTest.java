package com.example.itborrow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:google_login;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none", "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "app.oauth.google.enabled=true", "app.oauth.google.client-id=test-client", "app.oauth.google.client-secret=test-secret", "spring.flyway.enabled=false", "management.health.mail.enabled=false", "app.jobs.enabled=false",
        "app.mail.from=tests@example.test", "spring.mail.host=smtp.example.test", "spring.sql.init.mode=always",
        "spring.jpa.show-sql=false", "logging.level.org.hibernate.SQL=warn",
        "borrow.overdue.initial-delay-ms=86400000", "logging.level.root=WARN", "logging.level.org.springframework=WARN",
        "debug=false"
})
@AutoConfigureMockMvc
class GoogleOAuthIntegrationTest {
    @org.springframework.test.context.bean.override.mockito.MockitoBean com.example.itborrow.service.avatar.ImageStorage images;
    @org.springframework.test.context.bean.override.mockito.MockitoBean org.springframework.mail.javamail.JavaMailSender mail;
    @Autowired MockMvc mvc;
    @Test void configuredGoogleProviderIsPubliclyDiscoverable() throws Exception {
        mvc.perform(get("/api/v1/auth/providers")).andExpect(status().isOk()).andExpect(jsonPath("$.google").value(true)).andExpect(jsonPath("$.kku").value(false));
    }
    @Test void googleSignInRedirectsToProviderWithStateAndOidcScope() throws Exception {
        mvc.perform(get("/oauth2/authorization/google")).andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location",org.hamcrest.Matchers.allOf(org.hamcrest.Matchers.startsWith("https://accounts.google.com/"),org.hamcrest.Matchers.containsString("state="),org.hamcrest.Matchers.containsString("openid"),org.hamcrest.Matchers.containsString("test-client"))));
    }
    @Test void callbackWithoutSavedAuthorizationStateCannotLogIn() throws Exception {
        mvc.perform(get("/login/oauth2/code/google").param("code","invalid").param("state","invalid"))
            .andExpect(redirectedUrl("/?googleLoginError=true"));
    }
}
