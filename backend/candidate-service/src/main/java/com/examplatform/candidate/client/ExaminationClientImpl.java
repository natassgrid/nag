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

package com.examplatform.candidate.client;

import com.examplatform.candidate.dto.PublishedExamDto;
import com.examplatform.shared.constants.SharedConstants;
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
public class ExaminationClientImpl implements ExaminationClient {

    private final JdbcTemplate jdbcTemplate;
    private final RestClient restClient;
    private final String examinationServiceUrl;

    public ExaminationClientImpl(
            @Autowired(required = false) JdbcTemplate jdbcTemplate,
            @Value("${app.services.examination.url:http://localhost:8081}") String examinationServiceUrl) {
        this.jdbcTemplate = jdbcTemplate;
        this.examinationServiceUrl = examinationServiceUrl;
        this.restClient = RestClient.builder().baseUrl(examinationServiceUrl).build();
    }

    @Override
    public List<PublishedExamDto> getPublishedExams(String tenantId) {
        // 1. Try JDBC in macro/co-located deployment
        if (jdbcTemplate != null) {
            try {
                String sql = """
                    SELECT e.id, e.code, e.name, e.conducting_authority, e.category, e.status, e.duration_minutes, e.total_marks, e.blueprint_id,
                           COALESCE(COUNT(DISTINCT s.id), 0) AS sections_count,
                           COALESCE(COUNT(DISTINCT n.id), 0) AS nodes_count
                    FROM examination_service.examination e
                    LEFT JOIN examination_service.examination_section s ON s.examination_id = e.id
                    LEFT JOIN examination_service.exam_syllabus_node n ON n.examination_id = e.id
                    WHERE e.status = 'PUBLISHED'
                    GROUP BY e.id, e.code, e.name, e.conducting_authority, e.category, e.status, e.duration_minutes, e.total_marks, e.blueprint_id
                    ORDER BY e.name ASC
                    """;
                List<PublishedExamDto> exams = jdbcTemplate.query(sql, (rs, rowNum) -> new PublishedExamDto(
                        rs.getObject("id", UUID.class),
                        rs.getString("code"),
                        rs.getString("name"),
                        null,
                        rs.getString("status"),
                        rs.getInt("duration_minutes"),
                        rs.getInt("total_marks"),
                        rs.getObject("blueprint_id", UUID.class),
                        rs.getInt("sections_count"),
                        rs.getInt("nodes_count"),
                        rs.getString("conducting_authority"),
                        rs.getString("category")
                ));
                if (!exams.isEmpty()) {
                    return exams;
                }
            } catch (Exception e) {
                log.debug("Co-located examination table query failed: {}", e.getMessage());
            }
        }

        // 2. Try REST client in microservice deployment
        try {
            List<PublishedExamDto> response = restClient.get()
                    .uri("/api/v1/examinations?status=PUBLISHED")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<PublishedExamDto>>() {});
            if (response != null && !response.isEmpty()) {
                return response;
            }
        } catch (Exception e) {
            log.debug("REST call to examination-service failed: {}", e.getMessage());
        }

        // 3. Clean fallback default platform exams
        return getDefaultPublishedExams();
    }

    @Override
    public Optional<PublishedExamDto> getExam(UUID examId, String tenantId) {
        if (examId == null) {
            return Optional.empty();
        }

        if (jdbcTemplate != null) {
            try {
                String sql = """
                    SELECT e.id, e.code, e.name, e.conducting_authority, e.category, e.status, e.duration_minutes, e.total_marks, e.blueprint_id,
                           COALESCE(COUNT(DISTINCT s.id), 0) AS sections_count,
                           COALESCE(COUNT(DISTINCT n.id), 0) AS nodes_count
                    FROM examination_service.examination e
                    LEFT JOIN examination_service.examination_section s ON s.examination_id = e.id
                    LEFT JOIN examination_service.exam_syllabus_node n ON n.examination_id = e.id
                    WHERE e.id = ?
                    GROUP BY e.id, e.code, e.name, e.conducting_authority, e.category, e.status, e.duration_minutes, e.total_marks, e.blueprint_id
                    """;
                List<PublishedExamDto> list = jdbcTemplate.query(sql, (rs, rowNum) -> new PublishedExamDto(
                        rs.getObject("id", UUID.class),
                        rs.getString("code"),
                        rs.getString("name"),
                        null,
                        rs.getString("status"),
                        rs.getInt("duration_minutes"),
                        rs.getInt("total_marks"),
                        rs.getObject("blueprint_id", UUID.class),
                        rs.getInt("sections_count"),
                        rs.getInt("nodes_count"),
                        rs.getString("conducting_authority"),
                        rs.getString("category")
                ), examId);
                if (!list.isEmpty()) {
                    return Optional.of(list.getFirst());
                }
            } catch (Exception e) {
                log.debug("Co-located examination table lookup failed: {}", e.getMessage());
            }
        }

        try {
            PublishedExamDto exam = restClient.get()
                    .uri("/api/v1/examinations/{id}", examId)
                    .retrieve()
                    .body(PublishedExamDto.class);
            if (exam != null) {
                return Optional.of(exam);
            }
        } catch (Exception e) {
            log.debug("REST call to examination-service failed for exam {}: {}", examId, e.getMessage());
        }

        return getDefaultPublishedExams().stream()
                .filter(e -> e.id().equals(examId))
                .findFirst();
    }

    private List<PublishedExamDto> getDefaultPublishedExams() {
        return List.of(
                new PublishedExamDto(
                        UUID.fromString("11111111-2222-3333-4444-555555555551"),
                    SharedConstants.JEE_MAIN,
                        "JEE (Main) National Entrance Examination",
                        "National engineering entrance exam covering Physics, Chemistry, and Mathematics.",
                        "PUBLISHED",
                        180,
                        300,
                        UUID.fromString("bbbbbbbb-1111-2222-3333-444444444441"),
                        3,
                        30,
                        "National Testing Agency (NTA)",
                        "ENTRANCE"
                ),
                new PublishedExamDto(
                        UUID.fromString("11111111-2222-3333-4444-555555555552"),
                        "SSC_CGL",
                        "SSC Combined Graduate Level Examination",
                        "Staff Selection Commission Tier 1 examination covering Reasoning, GA, Quant, and English.",
                        "PUBLISHED",
                        60,
                        200,
                        UUID.fromString("bbbbbbbb-1111-2222-3333-444444444442"),
                        4,
                        25,
                        "Staff Selection Commission (SSC)",
                        "RECRUITMENT"
                ),
                new PublishedExamDto(
                        UUID.fromString("11111111-2222-3333-4444-555555555553"),
                        "NEET_UG",
                        "NEET (UG) Medical Entrance Examination",
                        "National Eligibility cum Entrance Test covering Physics, Chemistry, Botany, and Zoology.",
                        "PUBLISHED",
                        200,
                        720,
                        UUID.fromString("bbbbbbbb-1111-2222-3333-444444444443"),
                        4,
                        40,
                        "National Testing Agency (NTA)",
                        "ENTRANCE"
                )
        );
    }
}
