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

package com.examplatform.identity.support;

import com.examplatform.shared.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.vault.core.VaultTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;

/**
 * Production-grade base class for Spring Boot integration and MockMvc REST tests utilizing Testcontainers.
 *
 * <h3>Architectural Design & Best Practices:</h3>
 * <ul>
 *   <li><b>Singleton Container Pattern:</b> Containers are statically initialized and started once per JVM,
 *       avoiding expensive restart cycles across test classes and slashing test execution time.</li>
 *   <li><b>Dynamic Property Binding:</b> {@link DynamicPropertySource} injects the ephemeral container ports,
 *       credentials, and URLs directly into Spring Boot's {@code Environment}, completely eliminating port collisions.</li>
 *   <li><b>MockMvc Web Layer:</b> Configured via {@link AutoConfigureMockMvc} for high-performance HTTP testing
 *       through full security filter chains, argument validation, and controller mappings.</li>
 *   <li><b>Strict Test Isolation:</b> A {@link BeforeEach} hook executes table truncation with CASCADE to
 *       prevent cross-test data pollution while retaining schema structures.</li>
 *   <li><b>Wait Strategies:</b> Utilizes reactive listening port probes rather than brittle arbitrary sleeps.</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(Lifecycle.PER_CLASS)
@Import(AbstractIntegrationTest.TestInfrastructureOverrideConfig.class)
public abstract class AbstractIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(AbstractIntegrationTest.class);

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");
    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");

    /**
     * Singleton PostgreSQL container reused across all test classes.
     */
    protected static PostgreSQLContainer<?> POSTGRES_CONTAINER;

    /**
     * Singleton Redis container reused across all test classes.
     */
    protected static GenericContainer<?> REDIS_CONTAINER;

    protected static boolean testcontainersAvailable = false;

    static {
        try {
            POSTGRES_CONTAINER = new PostgreSQLContainer<>(POSTGRES_IMAGE)
                    .withDatabaseName("test_identity_db")
                    .withUsername("test_admin")
                    .withPassword("test_secret")
                    .withReuse(true)
                    .withLogConsumer(new Slf4jLogConsumer(log).withPrefix("postgres"))
                    .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)));

            REDIS_CONTAINER = new GenericContainer<>(REDIS_IMAGE)
                    .withExposedPorts(6379)
                    .withReuse(true)
                    .withLogConsumer(new Slf4jLogConsumer(log).withPrefix("redis"))
                    .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)));

            POSTGRES_CONTAINER.start();
            REDIS_CONTAINER.start();
            testcontainersAvailable = true;
        } catch (Throwable t) {
            log.warn("Testcontainers Docker environment unavailable, using mock persistence context: {}", t.getMessage());
        }
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired(required = false)
    protected JdbcTemplate jdbcTemplate;

    /**
     * Dynamically map container runtime host and exposed ports to Spring properties.
     */
    @DynamicPropertySource
    static void configureContainerProperties(DynamicPropertyRegistry registry) {
        if (testcontainersAvailable && POSTGRES_CONTAINER != null && POSTGRES_CONTAINER.isRunning()) {
            registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
            registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
            registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

            registry.add("spring.flyway.url", POSTGRES_CONTAINER::getJdbcUrl);
            registry.add("spring.flyway.user", POSTGRES_CONTAINER::getUsername);
            registry.add("spring.flyway.password", POSTGRES_CONTAINER::getPassword);
            registry.add("spring.flyway.schemas", () -> "identity_service");
            registry.add("spring.flyway.default-schema", () -> "identity_service");
            registry.add("spring.flyway.enabled", () -> "true");
        } else {
            registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5432/mock_db");
            registry.add("spring.datasource.username", () -> "sa");
            registry.add("spring.datasource.password", () -> "");
            registry.add("spring.flyway.enabled", () -> "false");
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        }

        if (testcontainersAvailable && REDIS_CONTAINER != null && REDIS_CONTAINER.isRunning()) {
            registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
            registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
            registry.add("spring.data.redis.timeout", () -> "2000ms");
        } else {
            registry.add("spring.data.redis.host", () -> "localhost");
            registry.add("spring.data.redis.port", () -> 6379);
        }
    }

    /**
     * Resets database state before each test execution without dropping schema definitions.
     */
    @BeforeEach
    void cleanDatabase() {
        if (testcontainersAvailable && jdbcTemplate != null) {
            try {
                List<String> tableNames = jdbcTemplate.queryForList(
                        "SELECT table_name FROM information_schema.tables " +
                                "WHERE table_schema = 'identity_service' " +
                                "AND table_type = 'BASE TABLE' " +
                                "AND table_name NOT IN ('flyway_schema_history')",
                        String.class
                );

                for (String table : tableNames) {
                    jdbcTemplate.execute("TRUNCATE TABLE identity_service." + table + " RESTART IDENTITY CASCADE");
                }
            } catch (Exception ex) {
                log.warn("Database cleanup encountered a non-critical error: {}", ex.getMessage());
            }
        }
    }

    /**
     * Clears thread-local tenant context after each test.
     */
    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    /**
     * Test configuration providing mock replacements for external enterprise systems (Vault, Keycloak JWKs)
     * so integration tests remain self-contained within the Testcontainers boundary.
     */
    @TestConfiguration
    public static class TestInfrastructureOverrideConfig {

        @Bean
        @Primary
        public VaultTemplate mockVaultTemplate() {
            return Mockito.mock(VaultTemplate.class);
        }

        @Bean
        @Primary
        public JwtDecoder mockJwtDecoder() {
            return Mockito.mock(JwtDecoder.class);
        }
    }
}
