package com.examplatform.asset.config;

import com.examplatform.shared.auth.ServiceAccountTokenClient;
import com.examplatform.shared.auth.TokenResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides a no-op ServiceAccountTokenClient for local/dev environments.
 * In production, this would be replaced by a Keycloak client_credentials implementation.
 */
@Configuration
public class ServiceAccountConfig {

    @Bean
    public ServiceAccountTokenClient serviceAccountTokenClient() {
        return serviceId -> TokenResponse.builder()
                .accessToken("dev-service-token")
                .expiresIn(3600)
                .tokenType("Bearer")
                .build();
    }
}
