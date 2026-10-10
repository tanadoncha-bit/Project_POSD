package com.example.itborrow.service.jobs;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EmailDeliveryHandlerTest {
    @Test
    void verificationEmailIncludesButtonAndEscapesContent() {
        var html =
                EmailDeliveryHandler.html(
                        "Verify your LeadIT email",
                        "Instructions\n\nhttps://example.com/verify-email?token=abc");
        assertThat(html)
                .contains(
                        ">Verify email</a>",
                        "https://example.com/verify-email?token=abc",
                        "30 minutes");
        assertThat(EmailDeliveryHandler.html("<script>", "<unsafe>"))
                .contains("&lt;script&gt;", "&lt;unsafe&gt;");
    }
}
