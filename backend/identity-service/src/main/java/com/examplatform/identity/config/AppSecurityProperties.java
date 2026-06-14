package com.examplatform.identity.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {
    private int maxFailedAttempts = 5;
    private int lockoutWindowSeconds = 600;
    private int rateLimitAuthPerIpPerMinute = 10;
    private long jwtAccessTokenLifetimeSeconds = 900;
    private long jwtRefreshTokenLifetimeSeconds = 28800;
    private int sessionIdleTimeoutSeconds = 1800;
    /** When true, MFA and risk-based step-up authentication are enforced. When false, password-only login is allowed. */
    private boolean mfaEnabled = false;
}
