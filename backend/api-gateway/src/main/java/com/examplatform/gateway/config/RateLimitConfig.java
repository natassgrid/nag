package com.examplatform.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Rate limiting configuration using Redis token-bucket algorithm.
 * Default: 1,000 requests per minute per client (replenishRate=17 tokens/sec, burstCapacity=1000).
 * Key resolution is based on the client's IP address (or authenticated principal if available).
 */
@Configuration
public class RateLimitConfig {

    /**
     * Resolves the rate-limiting key from the request.
     * Uses the authenticated principal name if available, otherwise falls back to remote IP address.
     */
    @Bean
    public KeyResolver clientKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .map(principal -> principal.getName())
                .switchIfEmpty(Mono.justOrEmpty(
                        exchange.getRequest().getRemoteAddress() != null
                                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                                : "anonymous"
                ));
    }
}
