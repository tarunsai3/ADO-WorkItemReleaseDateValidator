package com.example.adowebhook.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebhookConfig {

    @Value("${ado.org.url.prefix}")
    private String adoOrgUrl;

    @Value("${ado.project.name}")
    private String projectName;

    @Value("${ado.personal.access.token}")
    private String personalAccessToken;

    public String getAdoOrgUrl() {
        return adoOrgUrl;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getPersonalAccessToken() {
        return personalAccessToken;
    }
}
