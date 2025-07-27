package com.example.adowebhook.service;

import com.example.adowebhook.config.WebhookConfig;
import com.example.adowebhook.handler.CustomExp;
import com.example.adowebhook.model.WebhookPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

@Service
public class WebhookService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);
    private final AdoWorkItemService adoService;
    private final WebhookConfig config;

    public WebhookService(AdoWorkItemService adoService, WebhookConfig config) {
        this.adoService = adoService;
        this.config = config;
    }

    public ResponseEntity<String> validateReleaseDate(WebhookPayload payload) {
        String orgUrl = sanitize(payload.getCollectionUrl());
        String projectName = sanitize(payload.getProjectName());
        int workItemId = payload.getWorkItemId();
        String newDateStr = sanitize(payload.getNewReleaseDate());
        String oldDateStr = sanitize(payload.getOldReleaseDate());

        // Validate organization URL
        if (orgUrl == null || !orgUrl.startsWith(config.getAdoOrgUrl())) {
            logger.warn("Blocked invalid orgUrl.");
            throw new CustomExp(HttpStatus.BAD_REQUEST, "Invalid Azure DevOps Org URL.");
        }logger.info("DEBUG: orgUrl='{}', expectedPrefix='{}'", orgUrl, config.getAdoOrgUrl());

        // Validate project name
        if (projectName == null || projectName.isEmpty()) {
            logger.warn("Blocked invalid projectName.");
            throw new CustomExp(HttpStatus.BAD_REQUEST, "Invalid project name.");
        }

        if (newDateStr == null || newDateStr.trim().isEmpty()) {
            logger.info("WorkItem {}: No ReleaseDate change", workItemId);
            return ResponseEntity.ok("No ReleaseDate change detected");
        }

        if (!adoService.fetchReleasableToggle(orgUrl, projectName, workItemId)) {
            logger.info("WorkItem {}: not releasable", workItemId);
            return ResponseEntity.ok("Work item not releasable");
        }

        try {
            Instant ts = Instant.parse(newDateStr);
            LocalDate newDate = ts.atZone(ZoneOffset.UTC).toLocalDate();
            LocalDate today = LocalDate.now();

            if (!newDate.isAfter(today)) {
                logger.warn("WorkItem {}: Invalid past ReleaseDate", workItemId);

                adoService.updateWorkItem(orgUrl, projectName, workItemId, oldDateStr);

                String safeComment = String.format(
                        "Release Date was invalid (past or today). Reverted to previous valid value.");
                adoService.addValidationComment(orgUrl, projectName, workItemId, safeComment);

                return ResponseEntity.badRequest().body("Invalid ReleaseDate, reverted.");
            }

            logger.info("WorkItem {}: Valid ReleaseDate validated", workItemId);
            return ResponseEntity.ok("ReleaseDate is valid");

        } catch (DateTimeParseException e) {
            logger.error("WorkItem {}: Failed to parse ReleaseDate", workItemId);
            throw new CustomExp(HttpStatus.BAD_REQUEST, "Invalid date format for ReleaseDate");
        } catch (CustomExp e) {
            throw e;
        } catch (Exception e) {
            logger.error("WorkItem {}: Unexpected error during validation", workItemId);
            throw new CustomExp(HttpStatus.INTERNAL_SERVER_ERROR, "Error validating ReleaseDate");
        }
    }

    private String sanitize(String input) {
        return input == null ? "" : input.replaceAll("[\n\r\t]", "_").trim();
    }
}
