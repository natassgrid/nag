package com.examplatform.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Dev/Docker JWT decoder — validates HS256 tokens signed by DevKeycloakService.
 * Uses the same shared secret as identity-service's DevKeycloakService.
 */
@Configuration
@Profile({"dev", "docker"})
public class DevJwtConfig {

    @Value("${app.jwt.secret:dev-jwt-secret-key-for-local-testing-minimum-32-chars}")
    private String jwtSecret;

    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        SecretKeySpec key = new SecretKeySpec(
                jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build();
    }
}
