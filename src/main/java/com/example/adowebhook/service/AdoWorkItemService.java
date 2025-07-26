package com.example.adowebhook.service;

import com.example.adowebhook.config.WebhookConfig;
import com.example.adowebhook.handler.CustomExp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class AdoWorkItemService {

    private static final Logger logger = LoggerFactory.getLogger(AdoWorkItemService.class);
    private static final String WORK_ITEM_URL_TEMPLATE = "%s/%s/_apis/wit/workitems/%d?api-version=6.0";
    private static final String COMMENT_URL_TEMPLATE = "%s/%s/_apis/wit/workItems/%d/comments?api-version=7.0-preview.3";

    private final WebhookConfig config;
    private final RestTemplate restTemplate;

    @Autowired
    public AdoWorkItemService(WebhookConfig config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
    }

    public boolean fetchReleasableToggle(String orgUrl, String projectName, int workItemId) {
        validateInputs(orgUrl, projectName);
        try {
            String url = String.format(WORK_ITEM_URL_TEMPLATE, sanitizeUrl(orgUrl), projectName, workItemId);
            HttpEntity<?> entity = new HttpEntity<>(createHeaders(MediaType.APPLICATION_JSON));
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getBody() != null && response.getBody().contains("\"NT.App.Releasable\":true");
        } catch (HttpClientErrorException e) {
            logger.error("Fetch toggle failed for {}: {}", workItemId, e.getStatusCode());
            throw new CustomExp(HttpStatus.BAD_GATEWAY, "Failed to fetch releasable toggle");
        } catch (Exception e) {
            logger.error("Unexpected error fetching toggle for {}: {}", workItemId, e.getMessage());
            throw new CustomExp(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching releasable toggle");
        }
    }

    public void updateWorkItem(String orgUrl, String projectName, int workItemId, String releaseDate) {
        validateInputs(orgUrl, projectName);
        try {
            String url = String.format(WORK_ITEM_URL_TEMPLATE, sanitizeUrl(orgUrl), projectName, workItemId);

            Map<String, Object> patchOp = new HashMap<>();
            patchOp.put("op", "replace");
            patchOp.put("path", "/fields/NT.Release.ReleaseDate");
            patchOp.put("value", releaseDate == null ? "" : sanitize(releaseDate));

            List<Map<String, Object>> patch = Collections.singletonList(patchOp);

            HttpEntity<List<Map<String, Object>>> entity = new HttpEntity<>(
                    patch, createHeaders(MediaType.valueOf("application/json-patch+json"))
            );

            restTemplate.exchange(url, HttpMethod.PATCH, entity, String.class);
            logger.info("Reverted ReleaseDate for workItem {}", workItemId);
        } catch (HttpClientErrorException e) {
            logger.error("Update failed for {}: {}", workItemId, e.getStatusCode());
            throw new CustomExp(HttpStatus.BAD_GATEWAY, "Failed to update work item");
        } catch (Exception e) {
            logger.error("Unexpected update error for {}: {}", workItemId, e.getMessage());
            throw new CustomExp(HttpStatus.INTERNAL_SERVER_ERROR, "Error updating work item");
        }
    }

    public void addValidationComment(String orgUrl, String projectName, int workItemId, String comment) {
        validateInputs(orgUrl, projectName);
        try {
            String url = String.format(COMMENT_URL_TEMPLATE, sanitizeUrl(orgUrl), projectName, workItemId);

            Map<String, String> payload = Collections.singletonMap("text", sanitize(comment));

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, createHeaders(MediaType.APPLICATION_JSON));

            restTemplate.postForEntity(url, entity, String.class);
            logger.info("Added validation comment to workItem {}", workItemId);
        } catch (HttpClientErrorException e) {
            logger.error("Commenting failed for {}: {}", workItemId, e.getStatusCode());
            throw new CustomExp(HttpStatus.BAD_GATEWAY, "Failed to add comment");
        } catch (Exception e) {
            logger.error("Unexpected commenting error for {}: {}", workItemId, e.getMessage());
            throw new CustomExp(HttpStatus.INTERNAL_SERVER_ERROR, "Error adding comment");
        }
    }

    private HttpHeaders createHeaders(MediaType contentType) {
        String token = config.getPersonalAccessToken();
        if (token == null || token.trim().isEmpty()) {
            throw new CustomExp(HttpStatus.UNAUTHORIZED, "Missing Azure DevOps PAT");
        }

        String auth = ":" + token;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.setContentType(contentType);
        return headers;
    }

    private void validateInputs(String orgUrl, String projectName) {
        if (orgUrl == null || projectName == null || orgUrl.trim().isEmpty() || projectName.trim().isEmpty()) {
            throw new CustomExp(HttpStatus.BAD_REQUEST, "Invalid ADO Org URL or project name");
        }
    }

    private String sanitize(String input) {
        return input == null ? "" : input.replaceAll("[\n\r\t]", "_").trim();
    }

    private String sanitizeUrl(String url) {
        return sanitize(url).replaceAll("[^a-zA-Z0-9:/._-]", "");
    }
}
