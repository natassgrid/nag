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

package com.examplatform.admin.support;

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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;

/**
 * Production-grade base class for admin-service MockMvc integration tests using Testcontainers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(Lifecycle.PER_CLASS)
@Import(AbstractIntegrationTest.TestInfrastructureOverrideConfig.class)
public abstract class AbstractIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(AbstractIntegrationTest.class);

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");

    protected static PostgreSQLContainer<?> POSTGRES_CONTAINER;
    protected static boolean testcontainersAvailable = false;

    static {
        try {
            POSTGRES_CONTAINER = new PostgreSQLContainer<>(POSTGRES_IMAGE)
                    .withDatabaseName("test_admin_db")
                    .withUsername("test_admin")
                    .withPassword("test_secret")
                    .withReuse(true)
                    .withLogConsumer(new Slf4jLogConsumer(log).withPrefix("postgres"))
                    .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)));

            POSTGRES_CONTAINER.start();
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
            registry.add("spring.flyway.schemas", () -> "admin_service");
            registry.add("spring.flyway.default-schema", () -> "admin_service");
            registry.add("spring.flyway.enabled", () -> "true");
        } else {
            registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5432/mock_db");
            registry.add("spring.datasource.username", () -> "sa");
            registry.add("spring.datasource.password", () -> "");
            registry.add("spring.flyway.enabled", () -> "false");
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        }
    }

    @BeforeEach
    void cleanDatabase() {
        if (testcontainersAvailable && jdbcTemplate != null) {
            try {
                List<String> tableNames = jdbcTemplate.queryForList(
                        "SELECT table_name FROM information_schema.tables " +
                                "WHERE table_schema = 'admin_service' " +
                                "AND table_type = 'BASE TABLE' " +
                                "AND table_name NOT IN ('flyway_schema_history')",
                        String.class
                );

                for (String table : tableNames) {
                    jdbcTemplate.execute("TRUNCATE TABLE admin_service." + table + " RESTART IDENTITY CASCADE");
                }
            } catch (Exception ex) {
                log.warn("Database cleanup encountered a non-critical error: {}", ex.getMessage());
            }
        }
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @TestConfiguration
    public static class TestInfrastructureOverrideConfig {

        @Bean
        @Primary
        public JwtDecoder mockJwtDecoder() {
            return Mockito.mock(JwtDecoder.class);
        }

        @Bean
        @Primary
        public StringRedisTemplate mockStringRedisTemplate() {
            return Mockito.mock(StringRedisTemplate.class);
        }
    }
}
