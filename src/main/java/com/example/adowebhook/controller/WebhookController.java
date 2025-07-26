package com.example.adowebhook.controller;

import com.example.adowebhook.model.WebhookPayload;
import com.example.adowebhook.service.WebhookService;
import com.example.adowebhook.handler.CustomExp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);
    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/validate")
    public ResponseEntity<String> handleWebhook(@RequestBody WebhookPayload payload) {
        try {
            return webhookService.validateReleaseDate(payload);
        } catch (CustomExp e) {
            logger.error("Validation error: {}", e.getMessage());
            return ResponseEntity.status(e.getStatus()).body("Validation Failed");
        }
    }
}
