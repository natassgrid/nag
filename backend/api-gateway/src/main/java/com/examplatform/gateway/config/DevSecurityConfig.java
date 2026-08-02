package com.examplatform.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Dev/Docker security config — validates JWTs using shared HMAC secret.
 * Active only when 'dev' or 'docker' profile is set.
 */
@Configuration
@EnableWebFluxSecurity
@Profile({"dev", "docker"})
public class DevSecurityConfig {

    @Value("${app.jwt.secret:dev-jwt-secret-key-for-local-testing-minimum-32-chars}")
    private String jwtSecret;

    @Bean
    @Primary
    public SecurityWebFilterChain devSecurityWebFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                .pathMatchers("/api/v1/identity/register", "/api/v1/identity/auth/**", "/api/v1/identity/otp/**").permitAll()
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtDecoder(devJwtDecoder()))
                .bearerTokenConverter(exchange -> {
                    // Don't extract Bearer token for public auth endpoints
                    String path = exchange.getRequest().getPath().value();
                    if (path.startsWith("/api/v1/identity/auth/") ||
                        path.startsWith("/api/v1/identity/register") ||
                        path.startsWith("/api/v1/identity/otp/")) {
                        return reactor.core.publisher.Mono.empty();
                    }
                    return new org.springframework.security.oauth2.server.resource.web.server.authentication.ServerBearerTokenAuthenticationConverter()
                            .convert(exchange);
                })
            );
        return http.build();
    }

    @Bean
    @Primary
    public ReactiveJwtDecoder devJwtDecoder() {
        SecretKeySpec key = new SecretKeySpec(
                jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(key)
                .macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256)
                .build();
    }
}
