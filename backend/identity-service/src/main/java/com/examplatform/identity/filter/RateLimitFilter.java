package com.examplatform.identity.filter;

import com.examplatform.identity.config.AppSecurityProperties;
import com.examplatform.identity.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Servlet filter that applies Redis token-bucket rate limiting to authentication endpoints.
 * Limits each IP to a configurable number of auth attempts per minute (default 10).
 * Returns HTTP 429 Too Many Requests when the limit is exceeded.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final AppSecurityProperties securityProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Only apply rate limiting to auth endpoints
        if (path.startsWith("/api/v1/identity/auth/")) {
            String ipAddress = getClientIp(request);
            int maxAttempts = securityProperties.getRateLimitAuthPerIpPerMinute();

            if (!rateLimiterService.isAllowed(ipAddress, maxAttempts)) {
                log.warn("Rate limit exceeded for IP [{}] on path [{}]", ipAddress, path);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(
                        "{\"status\":\"ERROR\",\"message\":\"Too many authentication attempts. Please try again later.\",\"timestamp\":\"%s\"}"
                                .formatted(LocalDateTime.now()));
                return; // short-circuit — do not proceed to controller
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
