package com.examplatform.identity.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("AppSecurityProperties — default values")
class AppSecurityPropertiesTest {

    @Test
    @DisplayName("all default values match the specification")
    void defaultValuesMatchSpec() {
        AppSecurityProperties props = new AppSecurityProperties();

        assertAll(
            () -> assertEquals(5,     props.getMaxFailedAttempts(),
                    "maxFailedAttempts must default to 5"),
            () -> assertEquals(600,   props.getLockoutWindowSeconds(),
                    "lockoutWindowSeconds must default to 600"),
            () -> assertEquals(10,    props.getRateLimitAuthPerIpPerMinute(),
                    "rateLimitAuthPerIpPerMinute must default to 10"),
            () -> assertEquals(900L,  props.getJwtAccessTokenLifetimeSeconds(),
                    "jwtAccessTokenLifetimeSeconds must default to 900"),
            () -> assertEquals(28800L, props.getJwtRefreshTokenLifetimeSeconds(),
                    "jwtRefreshTokenLifetimeSeconds must default to 28800"),
            () -> assertEquals(1800,  props.getSessionIdleTimeoutSeconds(),
                    "sessionIdleTimeoutSeconds must default to 1800")
        );
    }
}
