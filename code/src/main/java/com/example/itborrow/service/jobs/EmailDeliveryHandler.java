package com.example.itborrow.service.jobs;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import java.util.Map;
@Component
public class EmailDeliveryHandler implements DeliveryJobHandler {
    private final ObjectProvider<JavaMailSender> mail;
    private final String from,host;
    public EmailDeliveryHandler(ObjectProvider<JavaMailSender> mail,@Value("${app.mail.from:}") String from,@Value("${spring.mail.host:}") String host) {this.mail=mail;this.from=from;this.host=host;}
    public String kind() {return "EMAIL";}
    public boolean available() {return !from.isBlank() && !host.isBlank() && mail.getIfAvailable()!=null;}
    public void execute(Map<String,Object> job) {
        var message=new SimpleMailMessage();message.setFrom(from);message.setTo((String)job.get("recipient"));message.setSubject((String)job.get("subject"));message.setText((String)job.get("payload"));mail.getObject().send(message);
    }
}
