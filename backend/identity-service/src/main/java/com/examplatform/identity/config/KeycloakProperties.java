package com.examplatform.identity.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.keycloak")
public class KeycloakProperties {
    private String realm = "exam-realm";
    private String clientId = "exam-backend";
    private String clientSecret = "";
    private String adminClientId = "admin-cli";
    private String adminUsername = "admin";
    private String adminPassword = "admin_secret";
    private String serverUrl = "http://localhost:8080";
}
