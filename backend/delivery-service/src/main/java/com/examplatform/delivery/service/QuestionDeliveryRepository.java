/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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
import com.examplatform.shared.constants.QuestionTypeConstants;
import com.examplatform.shared.constants.SharedConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
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

    private static final String COLUMN_SUBJECT_SLUG = "subject_slug";
    private static final String COLUMN_TOTAL_QUESTIONS = "total_questions";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final QuestionDeliveryParser parser;

    /**
     * Specification grouping section identifier, display name, marks, and negative marks.
     */
    public record SectionSpec(
            String secId,
            String secName,
            double marks,
            double negMarks
    ) {}

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
                        String subject = secNode.path(SharedConstants.SUBJECT).asText("");
                        int count = secNode.path("questionCount").asInt(25);
                        double marks = secNode.path("marksPerQuestion").asDouble(2.0);
                        double negMarks = secNode.path("negativeMarksPerQuestion").asDouble(0.5);
                        String secId = "sec-" + secIdx;

                        SectionSpec spec = new SectionSpec(secId, secName, marks, negMarks);
                        List<QuestionDeliveryDto> secQuestions = fetchQuestionsForSubject(
                                subject, count, spec, usedQuestionIds, tenantId);
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
            SectionSpec sectionSpec,
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

        List<QuestionDeliveryDto> matched = queryQuestions(
                sql,
                new Object[]{tenantId, keyword, keyword, keyword, limit * 2},
                sectionSpec.secId(),
                sectionSpec.secName(),
                sectionSpec.marks(),
                sectionSpec.negMarks()
        );

        List<QuestionDeliveryDto> filtered = new ArrayList<>();
        collectUniqueQuestions(matched, filtered, usedIds, limit);

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
            List<QuestionDeliveryDto> extra = queryQuestions(
                    fallbackSql,
                    new Object[]{tenantId},
                    sectionSpec.secId(),
                    sectionSpec.secName(),
                    sectionSpec.marks(),
                    sectionSpec.negMarks()
            );
            collectUniqueQuestions(extra, filtered, usedIds, limit);
        }

        return filtered;
    }

    private void collectUniqueQuestions(
            List<QuestionDeliveryDto> source,
            List<QuestionDeliveryDto> destination,
            Set<UUID> usedIds,
            int limit
    ) {
        for (QuestionDeliveryDto q : source) {
            try {
                UUID qUuid = UUID.fromString(q.getId());
                if (!usedIds.contains(qUuid)) {
                    usedIds.add(qUuid);
                    destination.add(q);
                    if (destination.size() >= limit) {
                        break;
                    }
                }
            } catch (IllegalArgumentException _) {
                // Ignore invalid question UUID formatting
            }
        }
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

    public List<QuestionDeliveryDto> queryQuestions(
            String sql,
            Object[] params,
            String defaultSecId,
            String defaultSecName,
            double marks,
            double negMarks
    ) {
        try {
            return jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> mapRowToQuestionDeliveryDto(rs, rowNum, defaultSecId, defaultSecName, marks, negMarks),
                    params
            );
        } catch (Exception e) {
            log.error("Failed to query questions from question_service: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private QuestionDeliveryDto mapRowToQuestionDeliveryDto(
            ResultSet rs,
            int rowNum,
            String defaultSecId,
            String defaultSecName,
            double marks,
            double negMarks
    ) throws SQLException {
        UUID id = rs.getObject("id", UUID.class);
        String subject = rs.getString(SharedConstants.SUBJECT);
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

        ParsedQuestionOptions parsedOptions = parseOptionsForQuestion(id, optionsJson, answerKey, hasImages);

        Long subjectId = getSafeLong(rs, "subject_id");
        Long topicId = getSafeLong(rs, "topic_id");
        Long subtopicId = getSafeLong(rs, "subtopic_id");
        String subtopic = getSafeString(rs, "subtopic");

        String sId = defaultSecId != null ? defaultSecId : parser.resolveSectionId(subject, rowNum + 1);
        String sName = defaultSecName != null ? defaultSecName : parser.resolveSectionName(subject, rowNum + 1);

        return QuestionDeliveryDto.builder()
                .id(id != null ? id.toString() : UUID.randomUUID().toString())
                .text(content)
                .hasImages(parsedOptions.hasImages())
                .options(parsedOptions.options())
                .marks(marks)
                .negativeMarks(negMarks)
                .sectionId(sId)
                .sectionName(sName)
                .subjectId(subjectId)
                .subject(subject)
                .topicId(topicId)
                .topic(topic != null ? topic : "General")
                .subtopicId(subtopicId)
                .subtopic(subtopic)
                .correctOptionIndex(parsedOptions.correctOptionIndex())
                .explanation(explanation)
                .questionType(questionType != null ? questionType : QuestionTypeConstants.SINGLE_MCQ)
                .passageId(passageId)
                .passageContent(passageContent)
                .passageOrderIndex(passageOrderIndex)
                .build();
    }

    private Long getSafeLong(ResultSet rs, String column) {
        try {
            long val = rs.getLong(column);
            return rs.wasNull() ? null : val;
        } catch (SQLException _) {
            return null;
        }
    }

    private String getSafeString(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (SQLException _) {
            return null;
        }
    }

    record ParsedQuestionOptions(
            List<QuestionOptionDeliveryDto> options,
            Integer correctOptionIndex,
            boolean hasImages
    ) {}

    private ParsedQuestionOptions parseOptionsForQuestion(
            UUID id, String optionsJson, String answerKey, boolean defaultHasImages) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return new ParsedQuestionOptions(Collections.emptyList(), null, defaultHasImages);
        }
        try {
            JsonNode optArr = objectMapper.readTree(optionsJson);
            QuestionDeliveryParser.ParsedOptions parsed = parser.parseOptions(optArr, answerKey);
            return new ParsedQuestionOptions(
                    parsed.options(),
                    parsed.correctOptionIndex(),
                    defaultHasImages || parsed.hasImages()
            );
        } catch (Exception e) {
            log.warn("Error parsing options JSON for question {}: {}", id, e.getMessage());
            return new ParsedQuestionOptions(Collections.emptyList(), null, defaultHasImages);
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
            Map<UUID, QuestionDeliveryDto> qMap = buildQuestionMap(questions);

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

    private Map<UUID, QuestionDeliveryDto> buildQuestionMap(List<QuestionDeliveryDto> questions) {
        Map<UUID, QuestionDeliveryDto> qMap = new HashMap<>();
        for (QuestionDeliveryDto q : questions) {
            try {
                qMap.put(UUID.fromString(q.getId()), q);
            } catch (IllegalArgumentException _) {
                // Ignore non-UUID question ID formatting
            }
        }
        return qMap;
    }

    public List<UUID> resolveQuestionUuids(UUID targetId, String tenantId) {
        List<UUID> uids = resolveFromPaperTable(targetId, tenantId);
        if (!uids.isEmpty()) {
            return uids;
        }

        uids = resolvePracticeSetUuids(targetId, tenantId);
        if (!uids.isEmpty()) {
            return uids;
        }

        uids = resolveFromPracticeSessionTable(targetId, tenantId);
        if (!uids.isEmpty()) {
            return uids;
        }

        return resolveFromExamSessionTable(targetId, tenantId);
    }

    private List<UUID> resolveFromPaperTable(UUID targetId, String tenantId) {
        try {
            List<String> defJsons = jdbcTemplate.query(
                    "SELECT paper_definition_json FROM paper_generator.paper WHERE id = ? AND (tenant_id = ? OR tenant_id = 'default' OR tenant_id IS NULL)",
                    (rs, rowNum) -> rs.getString("paper_definition_json"),
                    targetId, tenantId
            );
            if (!defJsons.isEmpty() && defJsons.get(0) != null && !defJsons.get(0).isBlank()) {
                return parser.extractQuestionUuidsFromJson(defJsons.get(0));
            }
        } catch (Exception e) {
            log.debug("Paper lookup error for {}: {}", targetId, e.getMessage());
        }
        return Collections.emptyList();
    }

    private List<UUID> resolveFromPracticeSessionTable(UUID targetId, String tenantId) {
        try {
            List<UUID> practiceSetIds = jdbcTemplate.query(
                    "SELECT practice_set_id FROM practice_service.practice_session WHERE id = ? AND (tenant_id = ? OR tenant_id = 'default' OR tenant_id IS NULL)",
                    (rs, rowNum) -> rs.getObject("practice_set_id", UUID.class),
                    targetId, tenantId
            );
            if (!practiceSetIds.isEmpty() && practiceSetIds.get(0) != null) {
                return resolvePracticeSetUuids(practiceSetIds.get(0), tenantId);
            }
        } catch (Exception e) {
            log.debug("Practice session lookup error for {}: {}", targetId, e.getMessage());
        }
        return Collections.emptyList();
    }

    private List<UUID> resolveFromExamSessionTable(UUID targetId, String tenantId) {
        try {
            List<UUID> sessionPaperIds = jdbcTemplate.query(
                    "SELECT paper_id FROM delivery_service.exam_session WHERE session_id = ? AND (tenant_id = ? OR tenant_id = 'default' OR tenant_id IS NULL)",
                    (rs, rowNum) -> rs.getObject("paper_id", UUID.class),
                    targetId, tenantId
            );
            if (!sessionPaperIds.isEmpty() && sessionPaperIds.get(0) != null) {
                return resolveQuestionUuids(sessionPaperIds.get(0), tenantId);
            }
        } catch (Exception e) {
            log.debug("Exam session lookup error for {}: {}", targetId, e.getMessage());
        }
        return Collections.emptyList();
    }

    private List<UUID> resolvePracticeSetUuids(UUID practiceSetId, String tenantId) {
        try {
            List<String> practiceQuestionIds = jdbcTemplate.query(
                    "SELECT question_ids FROM practice_service.practice_set WHERE id = ? AND (tenant_id = ? OR tenant_id = 'default' OR tenant_id IS NULL)",
                    (rs, rowNum) -> rs.getString("question_ids"),
                    practiceSetId, tenantId
            );
            if (practiceQuestionIds.isEmpty()) {
                return Collections.emptyList();
            }

            String qIdsJson = practiceQuestionIds.get(0);
            if (qIdsJson != null && !qIdsJson.isBlank()) {
                List<UUID> uids = parser.extractQuestionUuidsFromJsonOrString(qIdsJson);
                if (!uids.isEmpty()) {
                    return uids;
                }
            }

            return resolvePracticeSetFallbackBySubject(practiceSetId, tenantId);
        } catch (Exception e) {
            log.debug("Practice set lookup error for {}: {}", practiceSetId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<UUID> resolvePracticeSetFallbackBySubject(UUID practiceSetId, String tenantId) {
        String sql = "SELECT " + COLUMN_SUBJECT_SLUG + ", " + COLUMN_TOTAL_QUESTIONS
                + " FROM practice_service.practice_set WHERE id = ? AND (tenant_id = ? OR tenant_id = 'default' OR tenant_id IS NULL)";
        List<?> metaList = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> Map.of(
                        COLUMN_SUBJECT_SLUG, rs.getString(COLUMN_SUBJECT_SLUG) != null ? rs.getString(COLUMN_SUBJECT_SLUG) : "",
                        COLUMN_TOTAL_QUESTIONS, rs.getInt(COLUMN_TOTAL_QUESTIONS)
                ),
                practiceSetId, tenantId
        );
        if (metaList == null || metaList.isEmpty()) {
            return Collections.emptyList();
        }

        Object first = metaList.get(0);
        if (first instanceof Map<?, ?> map) {
            String subjectSlug = map.get(COLUMN_SUBJECT_SLUG) != null ? map.get(COLUMN_SUBJECT_SLUG).toString() : null;
            Object totalQObj = map.get(COLUMN_TOTAL_QUESTIONS);
            int totalQ = (totalQObj instanceof Number n) ? n.intValue() : 25;
            int limit = totalQ > 0 ? totalQ : 25;
            if (subjectSlug != null && !subjectSlug.isBlank()) {
                return resolveQuestionUuidsBySubject(subjectSlug, limit);
            }
        }
        return Collections.emptyList();
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
