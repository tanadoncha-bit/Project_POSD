package com.example.itborrow.service.jobs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class BrevoEmailClient {
    private final String apiKey;
    private final RestClient client;

    @Autowired
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
        return RestClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .requestFactory(factory)
                .build();
    }

    public boolean available() {
        return !apiKey.isBlank();
    }

    public void send(String from, String recipient, String subject, String text, String html) {
        send(from, recipient, subject, text, html, UUID.randomUUID().toString());
    }

    public void send(
            String from,
            String recipient,
            String subject,
            String text,
            String html,
            String deliveryKey) {
        try {
            client.post()
                    .uri("/smtp/email")
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(
                            Map.of(
                                    "sender",
                                    Map.of("name", "LeadIT", "email", from),
                                    "to",
                                    List.of(Map.of("email", recipient)),
                                    "subject",
                                    subject,
                                    "textContent",
                                    text,
                                    "htmlContent",
                                    html,
                                    "headers",
                                    Map.of("idempotencyKey", deliveryKey)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == 400) {
                try {
                    var body = new ObjectMapper().readTree(error.getResponseBodyAsString());
                    if ("duplicate_parameter".equals(body.path("code").asText())
                            && body.path("message")
                                    .asText()
                                    .toLowerCase(Locale.ROOT)
                                    .contains("idempotency")) return;
                } catch (RuntimeException ignored) {
                }
            }
            throw error;
        }
    }
}
