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

package com.examplatform.delivery.service;

import com.examplatform.delivery.dto.QuestionDeliveryDto;
import com.examplatform.delivery.repository.ExamSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExamQuestionDeliveryService Unit Tests")
class ExamQuestionDeliveryServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ExamSessionRepository examSessionRepository;

    private ObjectMapper objectMapper;
    private ExamQuestionDeliveryService service;

    private static final UUID Q1_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID Q2_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PAPER_ID = UUID.fromString("01a0fd2b-ad54-7a84-abde-63d0080d6650");
    private static final UUID SESSION_ID = UUID.fromString("01a0fd3e-ae81-7c07-a1a1-1164d114f02a");
    private static final UUID PRACTICE_SET_ID = UUID.fromString("01a0fd1d-ed38-74fd-a812-e7160d029654");

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new ExamQuestionDeliveryService(jdbcTemplate, redisTemplate, objectMapper, examSessionRepository);
    }

    @Nested
    @DisplayName("JSON Question UUID Extraction Tests")
    class JsonExtractionTests {

        @Test
        @DisplayName("Extracts UUIDs from questionIds JSON array")
        void extractFromQuestionIdsObject() {
            String json = "{\"questionIds\": [\"" + Q1_ID + "\", \"" + Q2_ID + "\"]}";
            List<UUID> uids = service.extractQuestionUuidsFromJson(json);
            assertThat(uids).containsExactly(Q1_ID, Q2_ID);
        }

        @Test
        @DisplayName("Extracts UUIDs from raw string array")
        void extractFromRawArray() {
            String json = "[\"" + Q1_ID + "\", \"" + Q2_ID + "\"]";
            List<UUID> uids = service.extractQuestionUuidsFromJson(json);
            assertThat(uids).containsExactly(Q1_ID, Q2_ID);
        }

        @Test
        @DisplayName("Extracts UUIDs from questions object array with id field")
        void extractFromQuestionsObjectArray() {
            String json = "{\"questions\": [{\"id\": \"" + Q1_ID + "\"}, {\"questionId\": \"" + Q2_ID + "\"}]}";
            List<UUID> uids = service.extractQuestionUuidsFromJson(json);
            assertThat(uids).containsExactly(Q1_ID, Q2_ID);
        }

        @Test
        @DisplayName("Extracts UUIDs from comma-separated string")
        void extractFromCommaSeparatedString() {
            String raw = Q1_ID + ", " + Q2_ID;
            List<UUID> uids = service.extractQuestionUuidsFromJsonOrString(raw);
            assertThat(uids).containsExactly(Q1_ID, Q2_ID);
        }

        @Test
        @DisplayName("Handles invalid/blank JSON gracefully")
        void handlesBlankAndInvalid() {
            assertThat(service.extractQuestionUuidsFromJson(null)).isEmpty();
            assertThat(service.extractQuestionUuidsFromJson("")).isEmpty();
            assertThat(service.extractQuestionUuidsFromJson("invalid-json")).isEmpty();
        }
    }

    @Nested
    @DisplayName("getQuestionsForPaper Tests")
    class GetQuestionsForPaperTests {

        @Test
        @DisplayName("Successfully resolves questions from paper table")
        @SuppressWarnings("unchecked")
        void resolvesFromPaperTable() {
            String defJson = "{\"questionIds\": [\"" + Q1_ID + "\", \"" + Q2_ID + "\"]}";

            when(jdbcTemplate.query(
                    argThat(sql -> sql != null && sql.contains("paper_generator.paper")),
                    any(RowMapper.class),
                    eq(PAPER_ID),
                    eq("default")
            )).thenReturn(List.of(defJson));

            QuestionDeliveryDto q1 = QuestionDeliveryDto.builder().id(Q1_ID.toString()).text("Q1").build();
            QuestionDeliveryDto q2 = QuestionDeliveryDto.builder().id(Q2_ID.toString()).text("Q2").build();

            when(jdbcTemplate.query(
                    argThat(sql -> sql != null && sql.contains("question_service.question")),
                    any(RowMapper.class),
                    eq(Q1_ID),
                    eq(Q2_ID)
            )).thenReturn(List.of(q1, q2));

            List<QuestionDeliveryDto> result = service.getQuestionsForPaper(PAPER_ID, "default");
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo(Q1_ID.toString());
            assertThat(result.get(1).getId()).isEqualTo(Q2_ID.toString());
        }

        @Test
        @DisplayName("Falls back to practice_set table when paper table has no record")
        @SuppressWarnings("unchecked")
        void fallsBackToPracticeSetTable() {
            String practiceIdsJson = "[\"" + Q1_ID + "\", \"" + Q2_ID + "\"]";

            when(jdbcTemplate.query(
                    argThat(sql -> sql != null && sql.contains("paper_generator.paper")),
                    any(RowMapper.class),
                    eq(PAPER_ID),
                    eq("default")
            )).thenReturn(List.of());

            when(jdbcTemplate.query(
                    argThat(sql -> sql != null && sql.contains("practice_service.practice_set")),
                    any(RowMapper.class),
                    eq(PAPER_ID),
                    eq("default")
            )).thenReturn(List.of(practiceIdsJson));

            QuestionDeliveryDto q1 = QuestionDeliveryDto.builder().id(Q1_ID.toString()).text("Q1").build();
            QuestionDeliveryDto q2 = QuestionDeliveryDto.builder().id(Q2_ID.toString()).text("Q2").build();

            when(jdbcTemplate.query(
                    argThat(sql -> sql != null && sql.contains("question_service.question")),
                    any(RowMapper.class),
                    eq(Q1_ID),
                    eq(Q2_ID)
            )).thenReturn(List.of(q1, q2));

            List<QuestionDeliveryDto> result = service.getQuestionsForPaper(PAPER_ID, "default");
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo(Q1_ID.toString());
        }

        @Test
        @DisplayName("Falls back to practice_session table when targetId is a practice session ID")
        @SuppressWarnings("unchecked")
        void fallsBackToPracticeSessionTable() {
            String practiceIdsJson = "[\"" + Q1_ID + "\", \"" + Q2_ID + "\"]";

            when(jdbcTemplate.query(
                    argThat(sql -> sql != null && sql.contains("paper_generator.paper")),
                    any(RowMapper.class),
                    eq(SESSION_ID),
                    eq("default")
            )).thenReturn(List.of());

            when(jdbcTemplate.query(
                    argThat(sql -> sql != null && sql.contains("practice_service.practice_set")),
                    any(RowMapper.class),
                    eq(SESSION_ID),
                    eq("default")
            )).thenReturn(List.of());

            when(jdbcTemplate.query(
                    argThat(sql -> sql != null && sql.contains("practice_service.practice_session")),
                    any(RowMapper.class),
                    eq(SESSION_ID),
                    eq("default")
            )).thenReturn(List.of(PRACTICE_SET_ID));

            when(jdbcTemplate.query(
                    argThat(sql -> sql != null && sql.contains("practice_service.practice_set")),
                    any(RowMapper.class),
                    eq(PRACTICE_SET_ID),
                    eq("default")
            )).thenReturn(List.of(practiceIdsJson));

            QuestionDeliveryDto q1 = QuestionDeliveryDto.builder().id(Q1_ID.toString()).text("Q1").build();
            QuestionDeliveryDto q2 = QuestionDeliveryDto.builder().id(Q2_ID.toString()).text("Q2").build();

            when(jdbcTemplate.query(
                    argThat(sql -> sql != null && sql.contains("question_service.question")),
                    any(RowMapper.class),
                    eq(Q1_ID),
                    eq(Q2_ID)
            )).thenReturn(List.of(q1, q2));

            List<QuestionDeliveryDto> result = service.getQuestionsForPaper(SESSION_ID, "default");
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo(Q1_ID.toString());
        }
    }
}
