package com.example.itborrow.service.jobs;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.assertj.core.api.Assertions.*;

class BrevoEmailClientTest {
    @Test
    void sendsRecipientAndBothEmailFormatsOverHttps() {
        var builder = RestClient.builder().baseUrl("https://api.brevo.com/v3");
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = new BrevoEmailClient("test-key", builder.build());
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andExpect(header("api-key", "test-key"))
                .andExpect(jsonPath("$.sender.email").value("sender@example.test"))
                .andExpect(jsonPath("$.to[0].email").value("recipient@example.test"))
                .andExpect(jsonPath("$.htmlContent").value("<p>Verify</p>"))
                .andExpect(jsonPath("$.textContent").value("Verify"))
                .andRespond(withSuccess());
        client.send("sender@example.test", "recipient@example.test", "Subject", "Verify", "<p>Verify</p>");
        server.verify();
    }

    @Test
    void apiFailureIsPropagatedForQueueRetry() {
        var builder = RestClient.builder().baseUrl("https://api.brevo.com/v3");
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = new BrevoEmailClient("test-key", builder.build());
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email")).andRespond(withServerError());
        assertThatThrownBy(
                () -> client.send("sender@example.test", "recipient@example.test", "Subject", "Text", "HTML"))
                .isInstanceOf(org.springframework.web.client.RestClientException.class);
        server.verify();
    }
}
