/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 */

package com.examplatform.recommendation.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

@Slf4j
@Component
public class ExamSyllabusClientImpl implements ExamSyllabusClient {

    private final JdbcTemplate jdbcTemplate;
    private final RestClient restClient;

    public ExamSyllabusClientImpl(
            @Autowired(required = false) JdbcTemplate jdbcTemplate,
            @Value("${app.services.examination.url:http://localhost:8081}") String examinationServiceUrl) {
        this.jdbcTemplate = jdbcTemplate;
        this.restClient = RestClient.builder().baseUrl(examinationServiceUrl).build();
    }

    @Override
    public List<ExamSyllabusNodeDto> getSyllabusNodes(UUID examId) {
        if (jdbcTemplate != null && examId != null) {
            try {
                String sql = """
                    SELECT sn.subject_id, sn.topic_id, sn.node_id AS subtopic_id, COALESCE(sn.weightage_pct, 10.0) AS weight
                    FROM examination_service.exam_syllabus_node sn
                    WHERE sn.examination_id = ? AND sn.node_type = 'SUBTOPIC'
                    """;
                List<ExamSyllabusNodeDto> list = jdbcTemplate.query(sql, (rs, rowNum) -> new ExamSyllabusNodeDto(
                        rs.getObject("subject_id", Long.class),
                        "Subject " + rs.getLong("subject_id"),
                        rs.getObject("topic_id", Long.class),
                        "Topic " + rs.getLong("topic_id"),
                        rs.getObject("subtopic_id", Long.class),
                        "Subtopic " + rs.getLong("subtopic_id"),
                        rs.getDouble("weight")
                ), examId);
                if (!list.isEmpty()) {
                    return list;
                }
            } catch (Exception e) {
                log.debug("Co-located syllabus lookup failed: {}", e.getMessage());
            }
        }

        try {
            List<ExamSyllabusNodeDto> result = restClient.get()
                    .uri("/api/v1/examinations/{id}/syllabus", examId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ExamSyllabusNodeDto>>() {});
            if (result != null && !result.isEmpty()) {
                return result;
            }
        } catch (Exception e) {
            log.debug("REST call to examination syllabus failed: {}", e.getMessage());
        }

        return getDefaultSyllabusNodes(examId);
    }

    @Override
    public Map<Long, Double> getBlueprintWeights(UUID examId) {
        List<ExamSyllabusNodeDto> nodes = getSyllabusNodes(examId);
        double totalWeight = nodes.stream().mapToDouble(ExamSyllabusNodeDto::blueprintWeight).sum();
        if (totalWeight <= 0) totalWeight = 1.0;

        Map<Long, Double> normalizedWeights = new HashMap<>();
        for (ExamSyllabusNodeDto node : nodes) {
            normalizedWeights.put(node.subtopicId(), node.blueprintWeight() / totalWeight);
        }
        return normalizedWeights;
    }

    private List<ExamSyllabusNodeDto> getDefaultSyllabusNodes(UUID examId) {
        return List.of(
                new ExamSyllabusNodeDto(1L, "Reasoning", 10L, "Logical Deductions", 101L, "Syllogisms", 25.0),
                new ExamSyllabusNodeDto(1L, "Reasoning", 10L, "Logical Deductions", 102L, "Analogies", 15.0),
                new ExamSyllabusNodeDto(2L, "Quantitative Aptitude", 20L, "Arithmetic", 201L, "Percentages", 30.0),
                new ExamSyllabusNodeDto(2L, "Quantitative Aptitude", 20L, "Arithmetic", 202L, "Ratio and Proportion", 20.0),
                new ExamSyllabusNodeDto(3L, "General Science", 30L, "Chemistry", 301L, "Chemical Reactions", 10.0)
        );
    }
}
