package com.examplatform.shared.aot;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * EnvironmentPostProcessor that excludes Resilience4j RefreshScoped auto-configurations
 * across all services to prevent Spring Boot 4.x AOT code generation conflicts with RefreshScope.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GraalVmResilience4jEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String EXCLUDE_PROPERTY = "spring.autoconfigure.exclude";
    private static final String EXCLUDED_CLASSES =
            "io.github.resilience4j.springboot.bulkhead.autoconfigure.BulkheadRefreshScopedRegistryAutoConfiguration," +
            "io.github.resilience4j.springboot.circuitbreaker.autoconfigure.CircuitBreakerRefreshScopedRegistryAutoConfiguration," +
            "io.github.resilience4j.springboot.ratelimiter.autoconfigure.RateLimiterRefreshScopedRegistryAutoConfiguration," +
            "io.github.resilience4j.springboot.retry.autoconfigure.RetryRefreshScopedRegistryAutoConfiguration," +
            "io.github.resilience4j.springboot.timelimiter.autoconfigure.TimeLimiterRefreshScopedRegistryAutoConfiguration";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String existing = environment.getProperty(EXCLUDE_PROPERTY);
        String combined = (existing == null || existing.isBlank()) ? EXCLUDED_CLASSES : existing + "," + EXCLUDED_CLASSES;
        Map<String, Object> props = new HashMap<>();
        props.put(EXCLUDE_PROPERTY, combined);
        environment.getPropertySources().addFirst(new MapPropertySource("graalvmResilience4jExcludes", props));
    }
}
