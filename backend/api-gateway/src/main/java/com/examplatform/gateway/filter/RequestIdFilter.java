package com.examplatform.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Global filter that ensures every request has an X-Request-Id header.
 * If the incoming request does not include X-Request-Id, a new UUID is generated and added.
 * The X-Request-Id is also propagated to the response for traceability.
 */
@Component
public class RequestIdFilter implements GlobalFilter, Ordered {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String TENANT_ID_HEADER = "X-Tenant-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Add X-Request-Id if not present
        String requestId = request.getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
            request = request.mutate()
                    .header(REQUEST_ID_HEADER, requestId)
                    .build();
        }

        // Propagate X-Request-Id to response
        String finalRequestId = requestId;
        exchange.getResponse().getHeaders().add(REQUEST_ID_HEADER, finalRequestId);

        // Propagate X-Tenant-Id if present
        String tenantId = request.getHeaders().getFirst(TENANT_ID_HEADER);
        if (tenantId != null && !tenantId.isBlank()) {
            exchange.getResponse().getHeaders().add(TENANT_ID_HEADER, tenantId);
        }

        ServerWebExchange mutatedExchange = exchange.mutate().request(request).build();
        return chain.filter(mutatedExchange);
    }

    @Override
    public int getOrder() {
        // Execute early in the filter chain
        return -100;
    }
}
