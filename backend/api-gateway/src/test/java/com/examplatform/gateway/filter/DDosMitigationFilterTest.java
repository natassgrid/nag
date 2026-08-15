/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.gateway.filter;

import com.examplatform.gateway.config.DDoSProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DDosMitigationFilter}.
 * Validates that rate above threshold returns 429 and publishes alerts,
 * while rate below threshold passes through normally.
 */
@ExtendWith(MockitoExtension.class)
class DDosMitigationFilterTest {

    @Mock
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private GatewayFilterChain chain;

    @Captor
    private ArgumentCaptor<Map<String, Object>> alertCaptor;

    private DDoSProperties ddosProperties;
    private DDosMitigationFilter filter;

    @BeforeEach
    void setUp() {
        ddosProperties = new DDoSProperties();
        ddosProperties.setThreshold(10000);
        ddosProperties.setWindowSeconds(1);
        filter = new DDosMitigationFilter(reactiveRedisTemplate, kafkaTemplate, ddosProperties);
        when(reactiveRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void filter_belowThreshold_passesThrough() {
        // Given a request within rate limits
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/test")
                .remoteAddress(new InetSocketAddress("192.168.1.100", 8080))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(valueOperations.increment("ddos:ip:192.168.1.100")).thenReturn(Mono.just(5L));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // When
        filter.filter(exchange, chain).block();

        // Then: request passes through
        verify(chain).filter(exchange);
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void filter_firstRequest_setsTtlAndPassesThrough() {
        // Given the first request in a window (count = 1)
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/test")
                .remoteAddress(new InetSocketAddress("10.0.0.1", 8080))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(valueOperations.increment("ddos:ip:10.0.0.1")).thenReturn(Mono.just(1L));
        when(reactiveRedisTemplate.expire("ddos:ip:10.0.0.1", Duration.ofSeconds(1)))
                .thenReturn(Mono.just(true));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // When
        filter.filter(exchange, chain).block();

        // Then: TTL is set and request passes through
        verify(reactiveRedisTemplate).expire("ddos:ip:10.0.0.1", Duration.ofSeconds(1));
        verify(chain).filter(exchange);
    }

    @Test
    void filter_aboveThreshold_returns429AndPublishesAlert() {
        // Given a request that exceeds the rate limit
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/test")
                .remoteAddress(new InetSocketAddress("192.168.1.200", 8080))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(valueOperations.increment("ddos:ip:192.168.1.200")).thenReturn(Mono.just(10001L));

        // When
        filter.filter(exchange, chain).block();

        // Then: response is 429
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        // Then: alert is published to both topics
        verify(kafkaTemplate).send(eq("exam.audit.events"), eq("192.168.1.200"), alertCaptor.capture());
        verify(kafkaTemplate).send(eq("exam.notifications.outbound"), eq("192.168.1.200"), any());

        Map<String, Object> alert = alertCaptor.getValue();
        assertThat(alert.get("eventType")).isEqualTo("DDOS_DETECTED");
        assertThat(alert.get("ipAddress")).isEqualTo("192.168.1.200");
        assertThat(alert.get("severity")).isEqualTo("CRITICAL");

        // Then: request is NOT forwarded
        verify(chain, never()).filter(any());
    }
}
