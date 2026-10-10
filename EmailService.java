package com.mrattorneys.contactapi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String to;
    private final String from;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.mail.to}") String to,
                        @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.to = to;
        this.from = from;
    }

    public void sendInquiry(ContactRequest request) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setReplyTo(request.email());
        message.setSubject("New website inquiry");
        message.setText(
                "Name: " + request.name() + "\n"
              + "Email: " + request.email() + "\n"
              + "Phone: " + (request.phone() == null ? "-" : request.phone()) + "\n\n"
              + request.message());
        mailSender.send(message);
    }
}