package com.example.adowebhook.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookPayload {

    @JsonProperty("resource")
    private Resource resource;

    @JsonProperty("resourceContainers")
    private ResourceContainers resourceContainers;

    public int getWorkItemId() {
        return resource != null ? resource.workItemId : -1;
    }

    public String getOldReleaseDate() {
        return (resource != null && resource.fields != null && resource.fields.releaseDate != null)
                ? resource.fields.releaseDate.oldValue : null;
    }

    public String getNewReleaseDate() {
        return (resource != null && resource.fields != null && resource.fields.releaseDate != null)
                ? resource.fields.releaseDate.newValue : null;
    }

    public String getCollectionUrl() {
        return resourceContainers != null && resourceContainers.collection != null
                ? resourceContainers.collection.baseUrl
                : null;
    }

    public String getProjectName() {
        return resource != null && resource.fields != null ? resource.fields.teamProject : null;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Resource {
        private int workItemId;
        private Fields fields;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Fields {
        @JsonProperty("NT.Release.ReleaseDate")
        private DateValues releaseDate;

        @JsonProperty("System.TeamProject")
        private String teamProject;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DateValues {
        private String oldValue;
        private String newValue;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResourceContainers {
        private Container collection;
        private Container project;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Container {
            @JsonProperty("baseUrl")
            private String baseUrl;
        }
    }
}
