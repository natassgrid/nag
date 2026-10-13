// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.delivery.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Slf4j
@Component
public class MockExamAccessValidator {

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;
    private final ObjectMapper objectMapper;

    public MockExamAccessValidator(
            ObjectProvider<JdbcTemplate> jdbcTemplateProvider,
            @Autowired(required = false) ObjectMapper objectMapper) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
    }

    /**
     * Enforces prerequisite Gatekeeper validation before launching full-length mock exams.
     *
     * @param candidateId Candidate identifier
     * @param examId      Exam identifier
     * @throws ResponseStatusException HTTP 422 GATEKEEPER_REQUIRED if blocked
     */
    public void validateCandidateAccess(UUID candidateId, UUID examId) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate == null || candidateId == null || examId == null) {
            return;
        }

        if (!isMockExam(jdbcTemplate, examId)) {
            return;
        }

        checkGatekeeperPrerequisite(jdbcTemplate, candidateId);
    }

    private boolean isMockExam(JdbcTemplate jdbcTemplate, UUID examId) {
        try {
            if (!tableExists(jdbcTemplate, "examination_service", "examination")) {
                return false;
            }
            String examSql = "SELECT title, exam_type FROM examination_service.examination WHERE id = ?";
            Boolean mockFound = jdbcTemplate.query(examSql, rs -> {
                if (rs.next()) {
                    String title = rs.getString("title");
                    String type = rs.getString("exam_type");
                    return isMockText(title) || isMockText(type);
                }
                return false;
            }, examId);
            return Boolean.TRUE.equals(mockFound);
        } catch (Exception e) {
            log.debug("Could not verify exam type: {}", e.getMessage());
            return false;
        }
    }

    private boolean isMockText(String text) {
        return text != null && text.toLowerCase().contains("mock");
    }

    private boolean tableExists(JdbcTemplate jdbcTemplate, String schema, String table) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = ? AND table_name = ?)",
                Boolean.class,
                schema,
                table
        );
        return Boolean.TRUE.equals(exists);
    }

    private void checkGatekeeperPrerequisite(JdbcTemplate jdbcTemplate, UUID candidateId) {
        try {
            if (!tableExists(jdbcTemplate, "recommendation_service", "dynamic_learning_path")) {
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
                evaluateMilestoneGraph(graphJson, candidateId);
            }, candidateId);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.debug("Gatekeeper check bypassed or schema not reachable: {}", e.getMessage());
        }
    }

    private void evaluateMilestoneGraph(String graphJson, UUID candidateId) {
        if (graphJson == null || graphJson.isBlank()) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(graphJson);
            if (isGatekeeperRequired(root)) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "GATEKEEPER_REQUIRED: Candidate must complete Phase 3 Gatekeeper Test before accessing full mock examinations."
                );
            }
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception parseEx) {
            log.warn("Failed to parse DLP JSON for candidate {}: {}", candidateId, parseEx.getMessage());
        }
    }

    private boolean isGatekeeperRequired(JsonNode root) {
        boolean gatekeeperPassed = root.has("gatekeeperPassed") && root.get("gatekeeperPassed").asBoolean();
        if (gatekeeperPassed) {
            return false;
        }
        return hasGaps(root.get("conceptualGaps")) || hasGaps(root.get("applicationGaps"));
    }

    private boolean hasGaps(JsonNode gapsNode) {
        return gapsNode != null && gapsNode.size() > 0;
    }
}
