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
import com.examplatform.delivery.dto.QuestionOptionDeliveryDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Data access repository for querying examination questions, sections, and papers from PostgreSQL.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class QuestionDeliveryRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final QuestionDeliveryParser parser;

    public List<QuestionDeliveryDto> fetchQuestionsForExam(UUID examId, String tenantId) {
        if (jdbcTemplate == null) {
            return Collections.emptyList();
        }

        String sectionsJson = null;
        if (examId != null) {
            try {
                sectionsJson = jdbcTemplate.queryForObject(
                        "SELECT sections_json FROM examination_service.examination WHERE id = ?",
                        String.class,
                        examId
                );
            } catch (Exception e) {
                log.debug("No sections_json found for examId {}: {}", examId, e.getMessage());
            }
        }

        List<QuestionDeliveryDto> result = new ArrayList<>();
        Set<UUID> usedQuestionIds = new HashSet<>();

        if (sectionsJson != null && !sectionsJson.isBlank()) {
            try {
                JsonNode sectionsArr = objectMapper.readTree(sectionsJson);
                if (sectionsArr.isArray() && !sectionsArr.isEmpty()) {
                    int secIdx = 1;
                    for (JsonNode secNode : sectionsArr) {
                        String secName = secNode.path("name").asText("Section " + secIdx);
                        String subject = secNode.path("subject").asText("");
                        int count = secNode.path("questionCount").asInt(25);
                        double marks = secNode.path("marksPerQuestion").asDouble(2.0);
                        double negMarks = secNode.path("negativeMarksPerQuestion").asDouble(0.5);
                        String secId = "sec-" + secIdx;

                        List<QuestionDeliveryDto> secQuestions = fetchQuestionsForSubject(
                                subject, count, secId, secName, marks, negMarks, usedQuestionIds, tenantId);
                        result.addAll(secQuestions);
                        secIdx++;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse sections_json for exam {}: {}", examId, e.getMessage());
            }
        }

        if (result.isEmpty()) {
            result = fetchDefaultApprovedQuestions(tenantId);
        }

        return result;
    }

    public List<QuestionDeliveryDto> fetchQuestionsForSubject(
            String subject,
            int limit,
            String secId,
            String secName,
            double marks,
            double negMarks,
            Set<UUID> usedIds,
            String tenantId
    ) {
        String keyword = "%" + (subject != null && !subject.isBlank() ? subject.trim() : "") + "%";
        String sql = """
            SELECT q.id, q.subject, q.topic, q.subtopic, q.difficulty, q.cognitive_level, q.question_type,
                   q.content, q.options, q.answer_key, q.explanation, q.has_images,
                   q.passage_id, p.content AS passage_content, q.passage_order_index
            FROM question_service.question q
            LEFT JOIN question_service.passage p ON q.passage_id = p.id
            LEFT JOIN question_service.translation t ON q.id = t.question_id AND t.language_code = 'hi' AND t.status IN ('PUBLISHED', 'APPROVED')
            WHERE (q.tenant_id = ? OR q.tenant_id = 'default')
              AND q.state = 'APPROVED'
              AND (q.subject ILIKE ? OR q.topic ILIKE ? OR q.subtopic ILIKE ?)
            ORDER BY (t.id IS NOT NULL) DESC, q.id
            LIMIT ?
            """;

        List<QuestionDeliveryDto> matched = queryQuestions(sql, new Object[]{tenantId, keyword, keyword, keyword, limit * 2}, secId, secName, marks, negMarks);

        List<QuestionDeliveryDto> filtered = new ArrayList<>();
        for (QuestionDeliveryDto q : matched) {
            try {
                UUID qUuid = UUID.fromString(q.getId());
                if (!usedIds.contains(qUuid)) {
                    usedIds.add(qUuid);
                    filtered.add(q);
                    if (filtered.size() >= limit) {
                        break;
                    }
                }
            } catch (Exception ignored) {}
        }

        if (filtered.size() < limit) {
            String fallbackSql = """
                SELECT q.id, q.subject, q.topic, q.subtopic, q.difficulty, q.cognitive_level, q.question_type,
                       q.content, q.options, q.answer_key, q.explanation, q.has_images,
                       q.passage_id, p.content AS passage_content, q.passage_order_index
                FROM question_service.question q
                LEFT JOIN question_service.passage p ON q.passage_id = p.id
                LEFT JOIN question_service.translation t ON q.id = t.question_id AND t.language_code = 'hi' AND t.status IN ('PUBLISHED', 'APPROVED')
                WHERE (q.tenant_id = ? OR q.tenant_id = 'default')
              AND q.state = 'APPROVED'
                ORDER BY (t.id IS NOT NULL) DESC, q.id
                LIMIT 200
                """;
            List<QuestionDeliveryDto> extra = queryQuestions(fallbackSql, new Object[]{tenantId}, secId, secName, marks, negMarks);
            for (QuestionDeliveryDto q : extra) {
                try {
                    UUID qUuid = UUID.fromString(q.getId());
                    if (!usedIds.contains(qUuid)) {
                        usedIds.add(qUuid);
                        filtered.add(q);
                        if (filtered.size() >= limit) {
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        return filtered;
    }

    public List<QuestionDeliveryDto> fetchDefaultApprovedQuestions(String tenantId) {
        String sql = """
            SELECT q.id, q.subject, q.topic, q.subtopic, q.difficulty, q.cognitive_level, q.question_type,
                   q.content, q.options, q.answer_key, q.explanation, q.has_images,
                   q.passage_id, p.content AS passage_content, q.passage_order_index
            FROM question_service.question q
            LEFT JOIN question_service.passage p ON q.passage_id = p.id
            LEFT JOIN question_service.translation t ON q.id = t.question_id AND t.language_code = 'hi' AND t.status IN ('PUBLISHED', 'APPROVED')
            WHERE (q.tenant_id = ? OR q.tenant_id = 'default')
              AND q.state = 'APPROVED'
            ORDER BY (t.id IS NOT NULL) DESC, CASE
              WHEN q.subject ILIKE '%Reasoning%' OR q.subject ILIKE '%Intelligence%' THEN 1
              WHEN q.subject ILIKE '%Awareness%' OR q.subject ILIKE '%General Studies%' THEN 2
              WHEN q.subject ILIKE '%Quantitative%' OR q.subject ILIKE '%Math%' THEN 3
              WHEN q.subject ILIKE '%English%' THEN 4
              ELSE 5 END, q.id
            LIMIT 100
            """;
        return queryQuestions(sql, new Object[]{tenantId}, null, null, 2.0, 0.5);
    }

    public List<QuestionDeliveryDto> queryQuestions(String sql, Object[] params, String defaultSecId, String defaultSecName, double marks, double negMarks) {
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                UUID id = rs.getObject("id", UUID.class);
                String subject = rs.getString("subject");
                String topic = rs.getString("topic");
                String content = rs.getString("content");
                String questionType = rs.getString("question_type");
                String optionsJson = rs.getString("options");
                String answerKey = rs.getString("answer_key");
                String explanation = rs.getString("explanation");
                boolean hasImages = rs.getBoolean("has_images");

                UUID passageIdObj = rs.getObject("passage_id", UUID.class);
                String passageId = passageIdObj != null ? passageIdObj.toString() : null;
                String passageContent = rs.getString("passage_content");
                Integer passageOrderIndex = (Integer) rs.getObject("passage_order_index");

                List<QuestionOptionDeliveryDto> options = new ArrayList<>();
                Integer correctOptionIndex = null;

                if (optionsJson != null && !optionsJson.isBlank()) {
                    try {
                        JsonNode optArr = objectMapper.readTree(optionsJson);
                        if (optArr.isArray()) {
                            for (int i = 0; i < optArr.size(); i++) {
                                JsonNode optNode = optArr.get(i);
                                String optText;
                                String optId = "";
                                String optImageUrl = null;
                                String optImageAltText = null;
                                boolean isCorrect = false;

                                if (optNode.isObject()) {
                                    optText = optNode.has("text") ? optNode.get("text").asText() : optNode.asText();
                                    optId = optNode.has("id") ? optNode.get("id").asText() : String.valueOf((char) ('A' + i));
                                    isCorrect = optNode.has("isCorrect") && optNode.get("isCorrect").asBoolean();
                                    optImageUrl = optNode.has("imageUrl") && !optNode.get("imageUrl").isNull() ? optNode.get("imageUrl").asText(null) : null;
                                    optImageAltText = optNode.has("imageAltText") && !optNode.get("imageAltText").isNull() ? optNode.get("imageAltText").asText(null) : null;
                                } else {
                                    optText = optNode.asText();
                                    optId = String.valueOf((char) ('A' + i));
                                }

                                if (optImageUrl != null && !optImageUrl.isBlank()) {
                                    hasImages = true;
                                }

                                if (isCorrect || (answerKey != null && (answerKey.equalsIgnoreCase(optId) || answerKey.equalsIgnoreCase(optText)))) {
                                    correctOptionIndex = i;
                                }

                                options.add(QuestionOptionDeliveryDto.builder()
                                        .id(optId)
                                        .index(i)
                                        .originalIndex(i)
                                        .text(optText)
                                        .imageUrl(optImageUrl)
                                        .imageAltText(optImageAltText)
                                        .build());
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Error parsing options JSON for question {}: {}", id, e.getMessage());
                    }
                }

                String sId = defaultSecId != null ? defaultSecId : parser.resolveSectionId(subject, rowNum + 1);
                String sName = defaultSecName != null ? defaultSecName : parser.resolveSectionName(subject, rowNum + 1);

                return QuestionDeliveryDto.builder()
                        .id(id != null ? id.toString() : UUID.randomUUID().toString())
                        .text(content)
                        .hasImages(hasImages)
                        .options(options)
                        .marks(marks)
                        .negativeMarks(negMarks)
                        .sectionId(sId)
                        .sectionName(sName)
                        .topic(topic != null ? topic : "General")
                        .correctOptionIndex(correctOptionIndex)
                        .explanation(explanation)
                        .questionType(questionType != null ? questionType : "SINGLE_MCQ")
                        .passageId(passageId)
                        .passageContent(passageContent)
                        .passageOrderIndex(passageOrderIndex)
                        .build();
            }, params);
        } catch (Exception e) {
            log.error("Failed to query questions from question_service: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<QuestionDeliveryDto> fetchQuestionsForPaper(UUID paperId, String tenantId) {
        if (jdbcTemplate == null || paperId == null) {
            return Collections.emptyList();
        }
        try {
            List<UUID> qUuids = resolveQuestionUuids(paperId, tenantId);
            if (qUuids.isEmpty()) {
                log.debug("No question IDs found for paper/practice set {}", paperId);
                return Collections.emptyList();
            }

            String inSql = String.join(",", Collections.nCopies(qUuids.size(), "?"));
            String sql = String.format("""
                SELECT q.id, q.subject, q.topic, q.subtopic, q.difficulty, q.cognitive_level, q.question_type,
                       q.content, q.options, q.answer_key, q.explanation, q.has_images,
                       q.passage_id, p.content AS passage_content, q.passage_order_index
                FROM question_service.question q
                LEFT JOIN question_service.passage p ON q.passage_id = p.id
                WHERE q.id IN (%s)
                """, inSql);

            List<QuestionDeliveryDto> questions = queryQuestions(sql, qUuids.toArray(), null, null, 2.0, 0.5);

            Map<UUID, QuestionDeliveryDto> qMap = new HashMap<>();
            for (QuestionDeliveryDto q : questions) {
                try {
                    qMap.put(UUID.fromString(q.getId()), q);
                } catch (Exception ignored) {}
            }

            List<QuestionDeliveryDto> orderedQuestions = new ArrayList<>();
            for (UUID uid : qUuids) {
                QuestionDeliveryDto q = qMap.get(uid);
                if (q != null) {
                    orderedQuestions.add(q);
                }
            }
            return orderedQuestions;
        } catch (Exception e) {
            log.warn("Could not fetch paper questions for paperId {}: {}", paperId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<UUID> resolveQuestionUuids(UUID targetId, String tenantId) {
        List<UUID> uids = new ArrayList<>();

        // 1. Try paper_generator.paper table
        try {
            List<String> defJsons = jdbcTemplate.query(
                    "SELECT paper_definition_json FROM paper_generator.paper WHERE id = ? AND (tenant_id = ? OR tenant_id = 'default' OR tenant_id IS NULL)",
                    (rs, rowNum) -> rs.getString("paper_definition_json"),
                    targetId, tenantId
            );
            if (!defJsons.isEmpty() && defJsons.get(0) != null && !defJsons.get(0).isBlank()) {
                uids = parser.extractQuestionUuidsFromJson(defJsons.get(0));
            }
        } catch (Exception e) {
            log.debug("Paper lookup error for {}: {}", targetId, e.getMessage());
        }

        // 2. If not found or empty, try practice_service.practice_set table
        if (uids.isEmpty()) {
            try {
                List<String> practiceQuestionIds = jdbcTemplate.query(
                        "SELECT question_ids FROM practice_service.practice_set WHERE id = ? AND (tenant_id = ? OR tenant_id = 'default' OR tenant_id IS NULL)",
                        (rs, rowNum) -> rs.getString("question_ids"),
                        targetId, tenantId
                );
                if (!practiceQuestionIds.isEmpty() && practiceQuestionIds.get(0) != null && !practiceQuestionIds.get(0).isBlank()) {
                    uids = parser.extractQuestionUuidsFromJsonOrString(practiceQuestionIds.get(0));
                }
                // Fallback: if practice set question_ids was empty or "[]", resolve by subject_slug
                if (uids.isEmpty() && !practiceQuestionIds.isEmpty()) {
                    List<Map<String, Object>> metaList = jdbcTemplate.query(
                            "SELECT subject_slug, total_questions FROM practice_service.practice_set WHERE id = ? AND (tenant_id = ? OR tenant_id = 'default' OR tenant_id IS NULL)",
                            (rs, rowNum) -> Map.of(
                                    "subject_slug", rs.getString("subject_slug") != null ? rs.getString("subject_slug") : "",
                                    "total_questions", rs.getInt("total_questions")
                            ),
                            targetId, tenantId
                    );
                    if (!metaList.isEmpty()) {
                        String subjectSlug = (String) metaList.get(0).get("subject_slug");
                        Integer totalQ = (Integer) metaList.get(0).get("total_questions");
                        int limit = (totalQ != null && totalQ > 0) ? totalQ : 25;
                        if (subjectSlug != null && !subjectSlug.isBlank()) {
                            uids = resolveQuestionUuidsBySubject(subjectSlug, limit);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Practice set lookup error for {}: {}", targetId, e.getMessage());
            }
        }

        // 3. If not found or empty, check practice_service.practice_session table (targetId may be a practice session ID)
        if (uids.isEmpty()) {
            try {
                List<UUID> practiceSetIds = jdbcTemplate.query(
                        "SELECT practice_set_id FROM practice_service.practice_session WHERE id = ? AND (tenant_id = ? OR tenant_id = 'default' OR tenant_id IS NULL)",
                        (rs, rowNum) -> rs.getObject("practice_set_id", UUID.class),
                        targetId, tenantId
                );
                if (!practiceSetIds.isEmpty() && practiceSetIds.get(0) != null) {
                    UUID pSetId = practiceSetIds.get(0);
                    List<String> practiceQuestionIds = jdbcTemplate.query(
                            "SELECT question_ids FROM practice_service.practice_set WHERE id = ? AND (tenant_id = ? OR tenant_id = 'default' OR tenant_id IS NULL)",
                            (rs, rowNum) -> rs.getString("question_ids"),
                            pSetId, tenantId
                    );
                    if (!practiceQuestionIds.isEmpty() && practiceQuestionIds.get(0) != null && !practiceQuestionIds.get(0).isBlank()) {
                        uids = parser.extractQuestionUuidsFromJsonOrString(practiceQuestionIds.get(0));
                    }
                    if (uids.isEmpty()) {
                        uids = resolveQuestionUuids(pSetId, tenantId);
                    }
                }
            } catch (Exception e) {
                log.debug("Practice session lookup error for {}: {}", targetId, e.getMessage());
            }
        }

        // 4. If not found or empty, check delivery_service.exam_session table (targetId may be an exam session ID)
        if (uids.isEmpty()) {
            try {
                List<UUID> sessionPaperIds = jdbcTemplate.query(
                        "SELECT paper_id FROM delivery_service.exam_session WHERE session_id = ? AND (tenant_id = ? OR tenant_id = 'default' OR tenant_id IS NULL)",
                        (rs, rowNum) -> rs.getObject("paper_id", UUID.class),
                        targetId, tenantId
                );
                if (!sessionPaperIds.isEmpty() && sessionPaperIds.get(0) != null) {
                    uids = resolveQuestionUuids(sessionPaperIds.get(0), tenantId);
                }
            } catch (Exception e) {
                log.debug("Exam session lookup error for {}: {}", targetId, e.getMessage());
            }
        }

        return uids;
    }

    private List<UUID> resolveQuestionUuidsBySubject(String subjectSlug, int limit) {
        if (subjectSlug == null || subjectSlug.isBlank() || jdbcTemplate == null) {
            return Collections.emptyList();
        }
        String fullPattern = "%" + subjectSlug.replace('-', ' ').replace('_', ' ').trim() + "%";
        String firstToken = "%" + subjectSlug.split("[-_\\s]+")[0] + "%";
        try {
            return jdbcTemplate.query(
                    """
                    SELECT id FROM question_service.question
                    WHERE (subject ILIKE ? OR topic ILIKE ?)
                       OR (subject ILIKE ? OR topic ILIKE ?)
                    ORDER BY id
                    LIMIT ?
                    """,
                    (rs, rowNum) -> rs.getObject("id", UUID.class),
                    fullPattern, fullPattern, firstToken, firstToken, limit
            );
        } catch (Exception e) {
            log.debug("Subject-based question lookup error for {}: {}", subjectSlug, e.getMessage());
            return Collections.emptyList();
        }
    }
}
