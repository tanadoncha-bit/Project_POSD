package com.example.itborrow.service.jobs;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.util.HtmlUtils;
import java.util.Map;
@Component
public class EmailDeliveryHandler implements DeliveryJobHandler {
    private final ObjectProvider<JavaMailSender> mail;
    private final String from,host,provider;
    private final BrevoEmailClient brevo;
    public EmailDeliveryHandler(ObjectProvider<JavaMailSender> mail,
            @Value("${app.mail.from:}") String from,
            @Value("${spring.mail.host:}") String host,
            @Value("${app.mail.provider:smtp}") String provider, BrevoEmailClient brevo) {
        this.mail=mail;this.from=from;this.host=host;this.provider=provider;this.brevo=brevo;
        if (!java.util.Set.of("smtp", "brevo").contains(provider))
            throw new IllegalArgumentException("MAIL_PROVIDER must be smtp or brevo.");
    }
    public String kind() {return "EMAIL";}
    public boolean available() {
        return !from.isBlank() && ("brevo".equals(provider) ? brevo.available() : !host.isBlank() && mail.getIfAvailable()!=null);
    }
    public void execute(Map<String,Object> job) {
        String recipient = (String) job.get("recipient");
        String subject = (String) job.get("subject");
        String body = (String) job.get("payload");
        if ("brevo".equals(provider)) {
            brevo.send(from, recipient, subject, body, html(subject, body),
                    job.get("delivery_key") == null ? java.util.UUID.randomUUID().toString() : (String) job.get("delivery_key"));
            return;
        }
        try {
            var sender = mail.getObject();
            var message = sender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(new jakarta.mail.internet.InternetAddress(from, "LeadIT"));
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(body, html(subject, body));
            sender.send(message);
        } catch (jakarta.mail.MessagingException | java.io.UnsupportedEncodingException error) {
            throw new IllegalStateException("Unable to prepare email.", error);
        }
    }

    static String html(String subject, String body) {
        String content = "<p style='line-height:1.7;white-space:pre-wrap'>" + HtmlUtils.htmlEscape(body) + "</p>";
        if (("Verify your LeadIT email".equals(subject) || "Confirm your new LeadIT email".equals(subject))) {
            String link = body.substring(body.lastIndexOf("\n") + 1).trim();
            var uri = java.net.URI.create(link);
            if (!java.util.Set.of("https", "http").contains(uri.getScheme()) || uri.getHost() == null)
                throw new IllegalArgumentException("Invalid verification link.");
            String safeLink = HtmlUtils.htmlEscape(link);
            content = "<p style='line-height:1.7'>Confirm that this email belongs to you by clicking the button below.</p>"
                    + "<p align='center' style='margin:28px 0;text-align:center'><a href='" + safeLink + "' style='display:inline-block;background:#222;color:#fff;padding:14px 24px;border-radius:24px;text-decoration:none;font-weight:600'>Verify email</a></p>"
                    + "<p style='color:#666;line-height:1.7'>This link expires in 30 minutes. If you did not request this email, you can ignore it.</p>"
                    + "<p style='font-size:12px;color:#666'>If the button does not work, open this link:</p><p style='font-size:12px;word-break:break-all'><a href='" + safeLink + "'>" + safeLink + "</a></p>";
        }
        return "<!doctype html><html><body style='margin:0;background:#f5f6f8;font-family:Arial,sans-serif;color:#222'>"
                + "<table role='presentation' width='100%' cellspacing='0' cellpadding='0'><tr><td align='center' style='padding:32px 16px'>"
                + "<table role='presentation' width='100%' style='max-width:520px;background:#fff;border:1px solid #e5e7eb;border-radius:16px' cellpadding='28'><tr><td>"
                + "<p style='color:#ff7a21;font-size:22px;font-weight:700;margin-top:0'>LeadIT</p><h1 style='font-size:22px'>"
                + HtmlUtils.htmlEscape(subject) + "</h1>" + content + "</td></tr></table></td></tr></table></body></html>";

    }
}
