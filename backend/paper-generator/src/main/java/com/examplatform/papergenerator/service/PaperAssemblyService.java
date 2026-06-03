package com.examplatform.papergenerator.service;

import com.examplatform.papergenerator.client.QuestionBankClient;
import com.examplatform.papergenerator.domain.Paper;
import com.examplatform.papergenerator.dto.BlueprintRule;
import com.examplatform.papergenerator.dto.GapDetail;
import com.examplatform.papergenerator.dto.PaperGenerationRequest;
import com.examplatform.papergenerator.dto.QuestionSummary;
import com.examplatform.papergenerator.exception.InsufficientQuestionsException;
import com.examplatform.papergenerator.repository.PaperRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service responsible for blueprint-driven paper assembly.
 * Selects questions satisfying subject/topic/difficulty/cognitive ratios,
 * enforces reuse policies, computes difficulty scores and topic distribution.
 *
 * Validates: Requirements 8.1, 8.2, 8.3, 8.4
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaperAssemblyService {

    private static final String PAPER_EVENTS_TOPIC = "exam.paper.events";
    private static final String AUDIT_TOPIC = "exam.audit.events";
    private static final String STATUS_DRAFT = "DRAFT";

    private static final String REUSE_POLICY_NEVER = "NEVER";
    private static final String REUSE_POLICY_1_YEAR = "1_YEAR";
    private static final String REUSE_POLICY_2_YEARS = "2_YEARS";

    private final QuestionBankClient questionBankClient;
    private final PaperRepository paperRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Generates a paper from the given blueprint request.
     *
     * <ol>
     *   <li>For each blueprint rule, selects questions matching criteria from the question bank</li>
     *   <li>Enforces reuse policies: excludes questions violating their reuse window</li>
     *   <li>Computes difficulty score as average of selected questions' difficulty weights</li>
     *   <li>Builds topic distribution JSON</li>
     *   <li>Builds paper definition JSON (ordered list of question IDs)</li>
     *   <li>Creates Paper entity in DRAFT status</li>
     *   <li>Publishes async job result to Kafka</li>
     * </ol>
     *
     * @param request     the paper generation request with blueprint rules
     * @param generatedBy the UUID of the user generating the paper
     * @param tenantId    the tenant identifier
     * @return the saved Paper entity
     */
    public Paper generatePaper(PaperGenerationRequest request, UUID generatedBy, String tenantId) {
        log.info("Starting paper generation for examId={}, shiftId={}, tenantId={}",
                request.getExamId(), request.getShiftId(), tenantId);

        List<UUID> selectedQuestionIds = new ArrayList<>();
        List<QuestionSummary> allSelectedQuestions = new ArrayList<>();
        List<GapDetail> gapDetails = new ArrayList<>();

        for (BlueprintRule rule : request.getBlueprintRules()) {
            List<QuestionSummary> candidates = questionBankClient.findAvailableQuestions(
                    rule.getSubject(), rule.getTopic(),
                    rule.getDifficulty(), rule.getCognitiveLevel(), tenantId);

            // Enforce reuse policies
            List<QuestionSummary> eligible = candidates.stream()
                    .filter(q -> isEligibleForReuse(q))
                    .toList();

            // Select required number of questions
            List<QuestionSummary> selected = eligible.stream()
                    .limit(rule.getQuestionCount())
                    .toList();

            if (selected.size() < rule.getQuestionCount()) {
                log.warn("Insufficient questions for rule: subject={}, topic={}, difficulty={}, " +
                                "needed={}, available={}",
                        rule.getSubject(), rule.getTopic(), rule.getDifficulty(),
                        rule.getQuestionCount(), selected.size());
                gapDetails.add(GapDetail.builder()
                        .subject(rule.getSubject())
                        .topic(rule.getTopic())
                        .difficulty(rule.getDifficulty())
                        .needed(rule.getQuestionCount())
                        .available(selected.size())
                        .build());
            }

            selectedQuestionIds.addAll(selected.stream()
                    .map(QuestionSummary::getQuestionId)
                    .toList());
            allSelectedQuestions.addAll(selected);
        }

        // If any rules cannot be satisfied, throw exception with gap report
        if (!gapDetails.isEmpty()) {
            throw new InsufficientQuestionsException(
                    "Blueprint cannot be satisfied: insufficient questions for " + gapDetails.size() + " rule(s)",
                    gapDetails);
        }

        // Compute difficulty score
        double difficultyScore = computeDifficultyScore(allSelectedQuestions);

        // Build topic distribution
        Map<String, Long> topicDistribution = allSelectedQuestions.stream()
                .collect(Collectors.groupingBy(QuestionSummary::getTopic, Collectors.counting()));

        // Build paper definition and topic distribution JSON
        String paperDefinitionJson = toJson(selectedQuestionIds);
        String topicDistributionJson = toJson(topicDistribution);

        // Create Paper entity in DRAFT status
        Paper paper = Paper.builder()
                .examId(request.getExamId())
                .shiftId(request.getShiftId())
                .status(STATUS_DRAFT)
                .paperDefinitionJson(paperDefinitionJson)
                .difficultyScore(difficultyScore)
                .topicDistributionJson(topicDistributionJson)
                .generatedBy(generatedBy)
                .build();
        paper.setTenantId(tenantId);

        Paper savedPaper = paperRepository.save(paper);

        // Publish async job result to Kafka
        publishPaperEvent(savedPaper);

        // Publish PAPER_GENERATED audit event (fire-and-forget)
        publishAuditEvent("PAPER_GENERATED", savedPaper, tenantId);

        log.info("Paper generated successfully: paperId={}, questionCount={}, difficultyScore={}",
                savedPaper.getId(), selectedQuestionIds.size(), difficultyScore);

        return savedPaper;
    }

    /**
     * Checks whether a question is eligible for reuse based on its reuse policy.
     */
    boolean isEligibleForReuse(QuestionSummary question) {
        if (question.getReusePolicy() == null) {
            return true;
        }

        return switch (question.getReusePolicy()) {
            case REUSE_POLICY_NEVER -> question.getUsageCount() == 0;
            case REUSE_POLICY_1_YEAR -> isOutsideReuseWindow(question, 365);
            case REUSE_POLICY_2_YEARS -> isOutsideReuseWindow(question, 730);
            default -> true;
        };
    }

    private boolean isOutsideReuseWindow(QuestionSummary question, int days) {
        if (question.getLastUsedAt() == null) {
            return true;
        }
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        return question.getLastUsedAt().isBefore(cutoff);
    }

    /**
     * Computes difficulty score as the average of selected questions' difficulty weights.
     * EASY=1, MEDIUM=2, HARD=3.
     */
    double computeDifficultyScore(List<QuestionSummary> questions) {
        if (questions.isEmpty()) {
            return 0.0;
        }
        double totalWeight = questions.stream()
                .mapToDouble(q -> difficultyWeight(q.getDifficulty()))
                .sum();
        return totalWeight / questions.size();
    }

    private double difficultyWeight(String difficulty) {
        if (difficulty == null) {
            return 2.0; // default to MEDIUM
        }
        return switch (difficulty.toUpperCase()) {
            case "EASY" -> 1.0;
            case "MEDIUM" -> 2.0;
            case "HARD" -> 3.0;
            default -> 2.0;
        };
    }

    private void publishPaperEvent(Paper paper) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventType", "PAPER_GENERATED");
        event.put("paperId", paper.getId().toString());
        event.put("examId", paper.getExamId().toString());
        event.put("shiftId", paper.getShiftId());
        event.put("status", paper.getStatus());
        event.put("difficultyScore", paper.getDifficultyScore());
        event.put("timestamp", Instant.now().toString());

        kafkaTemplate.send(PAPER_EVENTS_TOPIC, paper.getId().toString(), event);
        log.debug("Published paper generation event to topic={}, paperId={}",
                PAPER_EVENTS_TOPIC, paper.getId());
    }

    /**
     * Publishes a paper audit event to the audit topic (fire-and-forget).
     */
    private void publishAuditEvent(String eventType, Paper paper, String tenantId) {
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("eventType", eventType);
            event.put("paperId", paper.getId().toString());
            event.put("examId", paper.getExamId().toString());
            event.put("shiftId", paper.getShiftId());
            event.put("tenantId", tenantId);
            event.put("occurredAt", Instant.now().toString());

            kafkaTemplate.send(AUDIT_TOPIC, paper.getId().toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish audit event [{}] for paper [{}]: {}",
                                    eventType, paper.getId(), ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Unexpected error publishing paper audit event [{}]: {}", eventType, e.getMessage());
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize to JSON", e);
            throw new RuntimeException("JSON serialization failed", e);
        }
    }
}
