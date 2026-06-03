package com.examplatform.gateway.filter;

import com.examplatform.gateway.config.DDoSProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Global filter that mitigates DDoS attacks by tracking per-origin (IP) request rates
 * using Redis atomic counters with a 1-second TTL window.
 * <p>
 * If a single origin exceeds the configured threshold (default 10,000 req/s),
 * the request is absorbed and a 429 Too Many Requests response is returned.
 * A security alert is published to Kafka within 60 seconds of detection.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DDosMitigationFilter implements GlobalFilter, Ordered {

    private static final String REDIS_KEY_PREFIX = "ddos:ip:";
    private static final String AUDIT_TOPIC = "exam.audit.events";
    private static final String NOTIFICATION_TOPIC = "exam.notifications.outbound";

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final DDoSProperties ddosProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String ipAddress = extractIpAddress(exchange);
        if (ipAddress == null) {
            return chain.filter(exchange);
        }

        String redisKey = REDIS_KEY_PREFIX + ipAddress;
        Duration windowDuration = Duration.ofSeconds(ddosProperties.getWindowSeconds());

        return reactiveRedisTemplate.opsForValue().increment(redisKey)
                .flatMap(count -> {
                    if (count == 1L) {
                        // First request in window — set TTL
                        return reactiveRedisTemplate.expire(redisKey, windowDuration)
                                .then(chain.filter(exchange));
                    } else if (count > ddosProperties.getThreshold()) {
                        // Rate exceeded — absorb request and publish alert
                        log.warn("DDoS threshold exceeded for IP {}: {} requests in {}s window",
                                ipAddress, count, ddosProperties.getWindowSeconds());
                        publishSecurityAlert(ipAddress, count);
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        return exchange.getResponse().setComplete();
                    }
                    return chain.filter(exchange);
                });
    }

    @Override
    public int getOrder() {
        // Execute before other filters to short-circuit DDoS traffic early
        return -200;
    }

    private String extractIpAddress(ServerWebExchange exchange) {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        return null;
    }

    private void publishSecurityAlert(String ipAddress, long requestCount) {
        Map<String, Object> alert = Map.of(
                "eventType", "DDOS_DETECTED",
                "ipAddress", ipAddress,
                "requestCount", requestCount,
                "threshold", ddosProperties.getThreshold(),
                "windowSeconds", ddosProperties.getWindowSeconds(),
                "timestamp", Instant.now().toString(),
                "severity", "CRITICAL"
        );

        try {
            kafkaTemplate.send(AUDIT_TOPIC, ipAddress, alert);
            kafkaTemplate.send(NOTIFICATION_TOPIC, ipAddress, alert);
        } catch (Exception e) {
            log.error("Failed to publish DDoS security alert for IP {}: {}", ipAddress, e.getMessage());
        }
    }
}
