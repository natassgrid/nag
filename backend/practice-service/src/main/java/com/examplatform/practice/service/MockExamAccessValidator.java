// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Slf4j
@Component
public class MockExamAccessValidator {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public MockExamAccessValidator(
            @Autowired(required = false) JdbcTemplate jdbcTemplate,
            @Autowired(required = false) ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
    }

    /**
     * Enforces prerequisite Gatekeeper validation:
     * If a candidate has an active DLP with unresolved conceptual/application gaps
     * and has not passed the Phase 3 Gatekeeper test, full mock test starts are blocked.
     *
     * @param candidateId candidate identifier
     * @throws ResponseStatusException HTTP 422 GATEKEEPER_REQUIRED if blocked
     */
    public void validateCandidateAccess(UUID candidateId) {
        if (jdbcTemplate == null || candidateId == null) {
            return;
        }

        try {
            Boolean tableExists = jdbcTemplate.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'recommendation_service' AND table_name = 'dynamic_learning_path')",
                    Boolean.class
            );
            if (!Boolean.TRUE.equals(tableExists)) {
                return;
            }

            String sql = """
                SELECT milestone_graph_json
                FROM recommendation_service.dynamic_learning_path
                WHERE candidate_id = ? AND status = 'ACTIVE'
                ORDER BY created_at DESC
                LIMIT 1
                """;

            jdbcTemplate.query(sql, rs -> {
                String graphJson = rs.getString("milestone_graph_json");
                if (graphJson != null && !graphJson.isBlank()) {
                    try {
                        JsonNode root = objectMapper.readTree(graphJson);
                        boolean gatekeeperPassed = root.has("gatekeeperPassed") && root.get("gatekeeperPassed").asBoolean();
                        if (!gatekeeperPassed) {
                            JsonNode conceptualGaps = root.get("conceptualGaps");
                            JsonNode applicationGaps = root.get("applicationGaps");
                            boolean hasGaps = (conceptualGaps != null && !conceptualGaps.isEmpty())
                                    || (applicationGaps != null && !applicationGaps.isEmpty());

                            if (hasGaps) {
                                throw new ResponseStatusException(
                                        HttpStatus.UNPROCESSABLE_ENTITY,
                                        "GATEKEEPER_REQUIRED: Candidate must complete Phase 3 Gatekeeper Test before accessing full mock examinations."
                                );
                            }
                        }
                    } catch (ResponseStatusException rse) {
                        throw rse;
                    } catch (Exception parseEx) {
                        log.warn("Failed to parse DLP JSON for candidate {}: {}", candidateId, parseEx.getMessage());
                    }
                }
            }, candidateId);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.debug("Gatekeeper check bypassed or schema not reachable: {}", e.getMessage());
        }
    }
}
