package com.example.itborrow.service.jobs;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Component
public class BrevoEmailClient {
    private final String apiKey;
    private final RestClient client;

    @org.springframework.beans.factory.annotation.Autowired
    public BrevoEmailClient(@Value("${app.mail.brevo-api-key:}") String apiKey) {
        this(apiKey, createClient());
    }

    BrevoEmailClient(String apiKey, RestClient client) {
        this.apiKey = apiKey;
        this.client = client;
    }

    private static RestClient createClient() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return RestClient.builder().baseUrl("https://api.brevo.com/v3").requestFactory(factory).build();
    }

    public boolean available() { return !apiKey.isBlank(); }

    public void send(String from, String recipient, String subject, String text, String html) {
        client.post().uri("/smtp/email").header("api-key", apiKey)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(Map.of("sender", Map.of("name", "LeadIT", "email", from),
                        "to", List.of(Map.of("email", recipient)), "subject", subject,
                        "textContent", text, "htmlContent", html))
                .retrieve().toBodilessEntity();
    }
}
