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
public class QuestionBankContentClientImpl implements QuestionBankContentClient {

    private final JdbcTemplate jdbcTemplate;
    private final RestClient restClient;

    public QuestionBankContentClientImpl(
            @Autowired(required = false) JdbcTemplate jdbcTemplate,
            @Value("${app.services.question-bank.url:http://localhost:8083}") String questionBankUrl) {
        this.jdbcTemplate = jdbcTemplate;
        this.restClient = RestClient.builder().baseUrl(questionBankUrl).build();
    }

    @Override
    public List<MicroLessonDto> getLessonsForSubtopic(Long subtopicId) {
        if (jdbcTemplate != null && subtopicId != null) {
            try {
                String sql = """
                    SELECT id, subtopic_id, lesson_type, difficulty_level, sequence_no, title, estimated_minutes
                    FROM question_service.subtopic_micro_lesson
                    WHERE subtopic_id = ? AND status = 'PUBLISHED'
                    ORDER BY sequence_no ASC
                    """;
                List<MicroLessonDto> list = jdbcTemplate.query(sql, (rs, rowNum) -> new MicroLessonDto(
                        rs.getObject("id", UUID.class),
                        rs.getLong("subtopic_id"),
                        rs.getString("lesson_type"),
                        rs.getString("difficulty_level"),
                        rs.getInt("sequence_no"),
                        rs.getString("title"),
                        rs.getInt("estimated_minutes")
                ), subtopicId);
                if (!list.isEmpty()) {
                    return list;
                }
            } catch (Exception e) {
                log.debug("Co-located micro-lesson query failed: {}", e.getMessage());
            }
        }

        try {
            List<MicroLessonDto> result = restClient.get()
                    .uri("/api/micro-lessons/subtopic/{id}", subtopicId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<MicroLessonDto>>() {});
            if (result != null && !result.isEmpty()) {
                return result;
            }
        } catch (Exception e) {
            log.debug("REST call to question-bank for micro-lessons failed: {}", e.getMessage());
        }

        return getDefaultLessons(subtopicId);
    }

    private List<MicroLessonDto> getDefaultLessons(Long subtopicId) {
        long sId = (subtopicId != null) ? subtopicId : 101L;
        return List.of(
                new MicroLessonDto(
                        UUID.nameUUIDFromBytes(("lesson-concept-" + sId).getBytes()),
                        sId,
                        "CONCEPT",
                        "FOUNDATION",
                        1,
                        "Core Conceptual Primer",
                        5
                ),
                new MicroLessonDto(
                        UUID.nameUUIDFromBytes(("lesson-worked-" + sId).getBytes()),
                        sId,
                        "WORKED_EXAMPLES",
                        "CORE",
                        2,
                        "Step-by-Step Worked Problems",
                        7
                ),
                new MicroLessonDto(
                        UUID.nameUUIDFromBytes(("lesson-traps-" + sId).getBytes()),
                        sId,
                        "TRAPS_AND_PITFALLS",
                        "CORE",
                        3,
                        "Common Traps & Speed Hacks",
                        4
                ),
                new MicroLessonDto(
                        UUID.nameUUIDFromBytes(("lesson-visual-" + sId).getBytes()),
                        sId,
                        "VISUAL",
                        "FOUNDATION",
                        4,
                        "Interactive Visual Demonstration",
                        6
                ),
                new MicroLessonDto(
                        UUID.nameUUIDFromBytes(("lesson-cheat-" + sId).getBytes()),
                        sId,
                        "CHEAT_SHEET",
                        "ADVANCED",
                        5,
                        "Quick Formula Recap & Edge Cases",
                        3
                )
        );
    }
}
