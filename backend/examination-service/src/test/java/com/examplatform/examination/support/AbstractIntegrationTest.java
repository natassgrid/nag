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

package com.examplatform.examination.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for examination-service integration tests using the Singleton Container pattern.
 * Containers start once per test run and remain active across all test classes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(AbstractIntegrationTest.class);

    private static PostgreSQLContainer<?> POSTGRES_CONTAINER;
    private static boolean testcontainersAvailable = false;

    static {
        try {
            POSTGRES_CONTAINER = new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("exam_platform")
                    .withUsername("exam_admin")
                    .withPassword("exam_secret");
            POSTGRES_CONTAINER.start();
            testcontainersAvailable = true;
        } catch (Throwable t) {
            log.warn("Testcontainers Docker environment unavailable, using mock persistence context: {}", t.getMessage());
        }
    }

    @MockitoBean
    protected JwtDecoder jwtDecoder;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired(required = false)
    protected JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (testcontainersAvailable && POSTGRES_CONTAINER != null && POSTGRES_CONTAINER.isRunning()) {
            registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
            registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
            registry.add("spring.datasource.hikari.schema", () -> "examination_service");
            registry.add("spring.flyway.url", POSTGRES_CONTAINER::getJdbcUrl);
            registry.add("spring.flyway.user", POSTGRES_CONTAINER::getUsername);
            registry.add("spring.flyway.password", POSTGRES_CONTAINER::getPassword);
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
                jdbcTemplate.execute("SET search_path TO examination_service, public");
                jdbcTemplate.execute(
                        "TRUNCATE TABLE " +
                                "examination_service.exam_application, " +
                                "examination_service.shift_seat_allocation, " +
                                "examination_service.exam_shift, " +
                                "examination_service.examination_centre, " +
                                "examination_service.examination_schedule, " +
                                "examination_service.examination " +
                                "RESTART IDENTITY CASCADE"
                );
            } catch (Exception ex) {
                log.warn("Database cleanup encountered non-critical exception: {}", ex.getMessage());
            }
        }
    }
}
