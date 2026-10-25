/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
 */

package com.examplatform.recommendation.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionBankContentClientImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ResultSet resultSet;

    @Test
    @DisplayName("Should query subtopic_micro_lesson without title column and derive title via RowMapper")
    void shouldQueryMicroLessonsWithoutTitleColumnAndDeriveTitle() throws Exception {
        QuestionBankContentClientImpl client = new QuestionBankContentClientImpl(jdbcTemplate, "http://localhost:8083");

        UUID lessonId = UUID.randomUUID();
        when(resultSet.getObject("id", UUID.class)).thenReturn(lessonId);
        when(resultSet.getLong("subtopic_id")).thenReturn(42L);
        when(resultSet.getString("lesson_type")).thenReturn("CONCEPT");
        when(resultSet.getString("difficulty_level")).thenReturn("CORE");
        when(resultSet.getInt("sequence_no")).thenReturn(1);
        when(resultSet.getInt("estimated_minutes")).thenReturn(6);

        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq(42L)))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0);
                    RowMapper<MicroLessonDto> mapper = invocation.getArgument(1);
                    // Ensure the query does not include title in the SELECT columns
                    assertThat(sql).doesNotContain("title,");
                    assertThat(sql).doesNotContain(", title");
                    assertThat(sql).contains("SELECT id, subtopic_id, lesson_type, difficulty_level, sequence_no, estimated_minutes");
                    MicroLessonDto dto = mapper.mapRow(resultSet, 0);
                    return List.of(dto);
                });

        List<MicroLessonDto> result = client.getLessonsForSubtopic(42L);

        assertThat(result).hasSize(1);
        MicroLessonDto dto = result.get(0);
        assertThat(dto.id()).isEqualTo(lessonId);
        assertThat(dto.subtopicId()).isEqualTo(42L);
        assertThat(dto.lessonType()).isEqualTo("CONCEPT");
        assertThat(dto.sequenceNo()).isEqualTo(1);
        assertThat(dto.title()).isEqualTo("Approach 1: Conceptual Foundation");
        assertThat(dto.estimatedMinutes()).isEqualTo(6);
    }
}
