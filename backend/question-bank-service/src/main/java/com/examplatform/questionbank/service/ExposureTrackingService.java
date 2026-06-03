package com.examplatform.questionbank.service;

import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Tracks question exposure/usage across exams and shifts.
 * Called by paper-generator after a question is selected for a paper.
 *
 * Validates: Requirements 4.8, 4.9
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ExposureTrackingService {

    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    /**
     * Tracks usage of a question in a specific exam and shift.
     * Increments usageCount, sets lastUsedAt to now, and appends
     * examId/shiftId to their respective JSONB lists.
     *
     * @param questionId the question that was used
     * @param examId     the exam the question was included in
     * @param shiftId    the shift identifier
     */
    public void trackUsage(UUID questionId, UUID examId, String shiftId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Question not found: " + questionId));

        // Increment usage count
        question.setUsageCount(question.getUsageCount() + 1);
        question.setLastUsedAt(LocalDateTime.now());

        // Append examId to usedInExamIdsJson
        List<String> examIds = parseJsonList(question.getUsedInExamIdsJson());
        examIds.add(examId.toString());
        question.setUsedInExamIdsJson(toJson(examIds));

        // Append shiftId to usedInShiftIdsJson
        List<String> shiftIds = parseJsonList(question.getUsedInShiftIdsJson());
        shiftIds.add(shiftId);
        question.setUsedInShiftIdsJson(toJson(shiftIds));

        questionRepository.save(question);
        log.info("Tracked usage for question={} in exam={} shift={}, usageCount={}",
                questionId, examId, shiftId, question.getUsageCount());
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse JSON list, starting fresh: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private String toJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.error("Failed to serialize list to JSON: {}", e.getMessage());
            return "[]";
        }
    }
}
