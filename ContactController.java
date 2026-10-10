package com.mrattorneys.contactapi;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"})
@RestController
@RequestMapping("/api")
public class ContactController {

    private final EmailService emailService;

    public ContactController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/contact")
    public ResponseEntity<Map<String, String>> submit(@Valid @RequestBody ContactRequest request) {
        try {
            emailService.sendInquiry(request);
            return ResponseEntity.ok(Map.of("status", "received"));
        } catch (MailException e) {
            System.err.println("Email failed: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("status", "error"));
        }
    }
}
