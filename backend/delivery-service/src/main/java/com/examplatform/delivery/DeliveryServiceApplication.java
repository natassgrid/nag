package com.examplatform.delivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Delivery Service.
 * Manages live exam sessions, shift-key decryption, and real-time delivery of
 * question papers to candidates.
 *
 * RedisRepositoriesAutoConfiguration is excluded because Redis is used only for
 * hot exam session state (via RedisTemplate), not for Spring Data Redis repositories.
 * Without this exclusion, Spring Data enters multi-store strict mode and scans every
 * JPA repository interface against both stores, adding significant startup latency.
 */
@SpringBootApplication(exclude = RedisRepositoriesAutoConfiguration.class)
@EnableJpaRepositories(basePackages = "com.examplatform.delivery.repository")
@EnableScheduling
public class DeliveryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeliveryServiceApplication.class, args);
    }
}
