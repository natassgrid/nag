package com.examplatform.identity.filter;

import com.examplatform.identity.config.AppSecurityProperties;
import com.examplatform.identity.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitFilter")
class RateLimitFilterTest {

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private AppSecurityProperties securityProperties;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private RateLimitFilter rateLimitFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.setRemoteAddr("192.168.1.100");
    }

    @Nested
    @DisplayName("Auth endpoint rate limiting")
    class AuthEndpointRateLimiting {

        @Test
        @DisplayName("allows request when count is under limit")
        void allowsRequestWhenUnderLimit() throws ServletException, IOException {
            request.setRequestURI("/api/v1/identity/auth/token");
            when(securityProperties.getRateLimitAuthPerIpPerMinute()).thenReturn(10);
            when(rateLimiterService.isAllowed("192.168.1.100", 10)).thenReturn(true);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(response.getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("returns 429 when rate limit is exceeded")
        void returns429WhenLimitExceeded() throws ServletException, IOException {
            request.setRequestURI("/api/v1/identity/auth/token");
            when(securityProperties.getRateLimitAuthPerIpPerMinute()).thenReturn(10);
            when(rateLimiterService.isAllowed("192.168.1.100", 10)).thenReturn(false);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(429);
            assertThat(response.getContentType()).isEqualTo("application/json");
            assertThat(response.getContentAsString()).contains("Too many authentication attempts");
            verify(filterChain, never()).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("Non-auth endpoints")
    class NonAuthEndpoints {

        @Test
        @DisplayName("does not apply rate limiting to non-auth endpoints")
        void doesNotRateLimitNonAuthEndpoints() throws ServletException, IOException {
            request.setRequestURI("/api/v1/identity/register");

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(rateLimiterService, never()).isAllowed(anyString(), anyInt());
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("IP extraction")
    class IpExtraction {

        @Test
        @DisplayName("uses X-Forwarded-For header when present")
        void usesXForwardedForHeader() throws ServletException, IOException {
            request.setRequestURI("/api/v1/identity/auth/token");
            request.addHeader("X-Forwarded-For", "10.0.0.1, 192.168.1.1");
            when(securityProperties.getRateLimitAuthPerIpPerMinute()).thenReturn(10);
            when(rateLimiterService.isAllowed("10.0.0.1", 10)).thenReturn(true);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(rateLimiterService).isAllowed("10.0.0.1", 10);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("falls back to remoteAddr when X-Forwarded-For is absent")
        void fallsBackToRemoteAddr() throws ServletException, IOException {
            request.setRequestURI("/api/v1/identity/auth/webauthn");
            request.setRemoteAddr("203.0.113.42");
            when(securityProperties.getRateLimitAuthPerIpPerMinute()).thenReturn(10);
            when(rateLimiterService.isAllowed("203.0.113.42", 10)).thenReturn(true);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(rateLimiterService).isAllowed("203.0.113.42", 10);
        }
    }
}
