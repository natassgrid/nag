package com.examplatform.response;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Response Service.
 * Captures, persists, and manages candidate responses during live exam sessions.
 * Supports auto-save, manual save, navigation-triggered saves, and offline sync.
 *
 * RedisRepositoriesAutoConfiguration is excluded because Redis is used only for
 * hot session state (via RedisTemplate), not for Spring Data Redis repositories.
 * Without this exclusion, Spring Data enters multi-store strict mode and scans every
 * JPA repository interface against both stores, adding significant startup latency.
 */
@SpringBootApplication(exclude = RedisRepositoriesAutoConfiguration.class)
@EnableJpaRepositories(basePackages = "com.examplatform.response.repository")
@EnableScheduling
public class ResponseServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResponseServiceApplication.class, args);
    }
}
