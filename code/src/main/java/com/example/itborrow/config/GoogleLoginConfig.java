package com.example.itborrow.config;
import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.*;
@Configuration(proxyBeanMethods=false)
@ConditionalOnProperty(name="app.oauth.google.enabled",havingValue="true")
public class GoogleLoginConfig {
    @Bean public ClientRegistrationRepository googleRegistration(@Value("${app.oauth.google.client-id:}") String id,@Value("${app.oauth.google.client-secret:}") String secret) {
        if(id.isBlank() || secret.isBlank()) throw new IllegalStateException("Google login requires GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET.");
        var google=CommonOAuth2Provider.GOOGLE.getBuilder("google").clientId(id).clientSecret(secret).scope("openid","profile","email").build();
        return new InMemoryClientRegistrationRepository(google);
    }
}
