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

package com.examplatform.papergenerator.service;

import com.examplatform.papergenerator.client.QuestionBankClient;
import com.examplatform.papergenerator.domain.Paper;
import com.examplatform.papergenerator.dto.BlueprintFeasibilityResponse;
import com.examplatform.papergenerator.dto.BlueprintRule;
import com.examplatform.papergenerator.dto.GapDetail;
import com.examplatform.papergenerator.dto.PaperGenerationRequest;
import com.examplatform.papergenerator.dto.QuestionSummary;
import com.examplatform.papergenerator.dto.RuleFeasibilityDetail;
import com.examplatform.papergenerator.exception.InsufficientQuestionsException;
import com.examplatform.papergenerator.repository.PaperRepository;
import com.examplatform.shared.messaging.EventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service responsible for blueprint-driven paper assembly and feasibility verification.
 * Selects questions satisfying subject/topic/difficulty/cognitive ratios,
 * enforces reuse policies, computes difficulty scores, and alerts admins on question deficits.
 *
 * Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5
 */
@Slf4j
@Service
@Transactional
public class PaperAssemblyService {

    private static final String PAPER_EVENTS_TOPIC = "exam.paper.events";
    private static final String AUDIT_TOPIC = "exam.audit.events";
    private static final String NOTIFICATION_TOPIC = "exam.notifications.outbound";
    private static final String STATUS_DRAFT = "DRAFT";

    private static final String REUSE_POLICY_NEVER = "NEVER";
    private static final String REUSE_POLICY_1_YEAR = "1_YEAR";
    private static final String REUSE_POLICY_2_YEARS = "2_YEARS";

    private final QuestionBankClient questionBankClient;
    private final PaperRepository paperRepository;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final ExaminationLookupService examinationLookupService;

    @Autowired
    public PaperAssemblyService(
            QuestionBankClient questionBankClient,
            PaperRepository paperRepository,
            EventPublisher eventPublisher,
            ObjectMapper objectMapper,
            @Nullable ExaminationLookupService examinationLookupService) {
        this.questionBankClient = questionBankClient;
        this.paperRepository = paperRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.examinationLookupService = examinationLookupService;
    }

    public PaperAssemblyService(
            QuestionBankClient questionBankClient,
            PaperRepository paperRepository,
            EventPublisher eventPublisher,
            ObjectMapper objectMapper) {
        this(questionBankClient, paperRepository, eventPublisher, objectMapper, null);
    }

    /**
     * Checks blueprint rules against current question bank availability without generating a paper.
     * Evaluates question reuse policies, computes surpluses/deficits, and optionally alerts admins on deficit.
     *
     * @param rules       the list of blueprint rules to evaluate
     * @param examId      optional exam UUID for context
     * @param shiftId     optional shift ID for context
     * @param tenantId    the tenant identifier
     * @param notifyAdmin whether to publish an admin alert if any rule deficit is detected
     * @return a comprehensive BlueprintFeasibilityResponse with rule-by-rule audit
     */
    @Transactional(readOnly = true)
    public BlueprintFeasibilityResponse checkBlueprintSufficiency(
            List<BlueprintRule> rules,
            @Nullable UUID examId,
            @Nullable String shiftId,
            String tenantId,
            boolean notifyAdmin) {

        log.info("Checking blueprint sufficiency: rulesCount={}, examId={}, shiftId={}, tenant={}",
                rules != null ? rules.size() : 0, examId, shiftId, tenantId);

        if (rules == null || rules.isEmpty()) {
            return BlueprintFeasibilityResponse.builder()
                    .feasible(true)
                    .examId(examId)
                    .shiftId(shiftId)
                    .totalQuestionsNeeded(0)
                    .totalQuestionsAvailable(0)
                    .deficitRuleCount(0)
                    .ruleDetails(List.of())
                    .gaps(List.of())
                    .notificationDispatched(false)
                    .summary("Blueprint contains no rules; trivially satisfied.")
                    .checkedAt(Instant.now())
                    .build();
        }

        List<RuleFeasibilityDetail> ruleDetails = new ArrayList<>();
        List<GapDetail> gapDetails = new ArrayList<>();
        int totalNeeded = 0;
        int totalAvailable = 0;

        for (BlueprintRule rule : rules) {
            int needed = rule.getQuestionCount();
            totalNeeded += needed;

            List<QuestionSummary> candidates = questionBankClient.findAvailableQuestions(
                    rule.getSubject(), rule.getTopic(),
                    rule.getDifficulty(), rule.getCognitiveLevel(), tenantId);

            List<QuestionSummary> eligible = candidates.stream()
                    .filter(this::isEligibleForReuse)
                    .toList();

            int available = eligible.size();
            totalAvailable += available;

            if (available < needed) {
                int deficit = needed - available;
                log.warn("Blueprint rule deficit detected: subject={}, topic={}, difficulty={}, needed={}, available={}, deficit={}",
                        rule.getSubject(), rule.getTopic(), rule.getDifficulty(), needed, available, deficit);

                GapDetail gap = GapDetail.builder()
                        .subject(rule.getSubject())
                        .topic(rule.getTopic())
                        .difficulty(rule.getDifficulty())
                        .needed(needed)
                        .available(available)
                        .build();
                gapDetails.add(gap);

                ruleDetails.add(RuleFeasibilityDetail.builder()
                        .subject(rule.getSubject())
                        .topic(rule.getTopic())
                        .difficulty(rule.getDifficulty())
                        .cognitiveLevel(rule.getCognitiveLevel())
                        .needed(needed)
                        .available(available)
                        .surplus(0)
                        .deficit(deficit)
                        .status("DEFICIT")
                        .build());
            } else {
                int surplus = available - needed;
                ruleDetails.add(RuleFeasibilityDetail.builder()
                        .subject(rule.getSubject())
                        .topic(rule.getTopic())
                        .difficulty(rule.getDifficulty())
                        .cognitiveLevel(rule.getCognitiveLevel())
                        .needed(needed)
                        .available(available)
                        .surplus(surplus)
                        .deficit(0)
                        .status("SATISFIED")
                        .build());
            }
        }

        boolean feasible = gapDetails.isEmpty();
        boolean notificationDispatched = false;

        if (!feasible && notifyAdmin) {
            publishInsufficientQuestionsAlert(gapDetails, examId, shiftId, tenantId);
            publishInsufficientQuestionsAuditEvent(gapDetails, examId, shiftId, tenantId);
            notificationDispatched = true;
        }

        String summary = feasible
                ? String.format("Blueprint is feasible. All %d rule(s) satisfied with %d total questions needed (%d available).",
                        rules.size(), totalNeeded, totalAvailable)
                : String.format("Blueprint is INSUFFICIENT. %d of %d rule(s) have question deficits (%d needed, %d available).",
                        gapDetails.size(), rules.size(), totalNeeded, totalAvailable);

        return BlueprintFeasibilityResponse.builder()
                .feasible(feasible)
                .examId(examId)
                .shiftId(shiftId)
                .totalQuestionsNeeded(totalNeeded)
                .totalQuestionsAvailable(totalAvailable)
                .deficitRuleCount(gapDetails.size())
                .ruleDetails(ruleDetails)
                .gaps(gapDetails)
                .notificationDispatched(notificationDispatched)
                .summary(summary)
                .checkedAt(Instant.now())
                .build();
    }

    /**
     * Generates a paper from the given blueprint request.
     *
     * <ol>
     *   <li>For each blueprint rule, selects questions matching criteria from the question bank</li>
     *   <li>Enforces reuse policies: excludes questions violating their reuse window</li>
     *   <li>Computes difficulty score as average of selected questions' difficulty weights</li>
     *   <li>Builds topic distribution JSON</li>
     *   <li>Builds paper definition JSON (ordered list of question IDs)</li>
     *   <li>Creates Paper entity in DRAFT status with meaningful name</li>
     *   <li>Publishes async job result to Kafka</li>
     * </ol>
     *
     * @param request     the paper generation request with blueprint rules
     * @param generatedBy the UUID of the user generating the paper
     * @param tenantId    the tenant identifier
     * @return the saved Paper entity
     */
    public Paper generatePaper(PaperGenerationRequest request, UUID generatedBy, String tenantId) {
        log.info("Starting paper generation for examId={}, shiftId={}, isPractice={}, tenantId={}",
                request.getExamId(), request.getShiftId(), request.getIsPractice(), tenantId);

        List<UUID> selectedQuestionIds = new ArrayList<>();
        List<QuestionSummary> allSelectedQuestions = new ArrayList<>();
        List<GapDetail> gapDetails = new ArrayList<>();

        for (BlueprintRule rule : request.getBlueprintRules()) {
            List<QuestionSummary> candidates = questionBankClient.findAvailableQuestions(
                    rule.getSubject(), rule.getTopic(),
                    rule.getDifficulty(), rule.getCognitiveLevel(), tenantId);

            // Enforce reuse policies
            List<QuestionSummary> eligible = candidates.stream()
                    .filter(this::isEligibleForReuse)
                    .toList();

            // Select required number of questions
            List<QuestionSummary> selected = eligible.stream()
                    .limit(rule.getQuestionCount())
                    .toList();

            if (selected.size() < rule.getQuestionCount()) {
                log.warn("Insufficient questions for rule: subject={}, topic={}, difficulty={}, needed={}, available={}",
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

        // If any rules cannot be satisfied, notify admin and throw exception with gap report
        if (!gapDetails.isEmpty()) {
            publishInsufficientQuestionsAlert(gapDetails, request.getExamId(), request.getShiftId(), tenantId);
            publishInsufficientQuestionsAuditEvent(gapDetails, request.getExamId(), request.getShiftId(), tenantId);

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
        // Store as {"questionIds": [...]} so PaperSerializer can extract IDs correctly
        String paperDefinitionJson = toJson(Map.of("questionIds", selectedQuestionIds));
        String topicDistributionJson = toJson(topicDistribution);

        // Resolve meaningful paper name
        String paperName = resolvePaperName(request);

        // Create Paper entity in DRAFT status
        Paper paper = Paper.builder()
                .name(paperName)
                .examId(request.getExamId())
                .shiftId(request.getShiftId())
                .status(STATUS_DRAFT)
                .isPractice(Boolean.TRUE.equals(request.getIsPractice()))
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

        log.info("Paper generated successfully: paperId={}, name='{}', isPractice={}, questionCount={}, difficultyScore={}",
                savedPaper.getId(), savedPaper.getName(), savedPaper.isPractice(), selectedQuestionIds.size(), difficultyScore);

        return savedPaper;
    }

    private String resolvePaperName(PaperGenerationRequest request) {
        if (request.getName() != null && !request.getName().isBlank()) {
            return request.getName().trim();
        }

        String examName = null;
        if (request.getExamId() != null && examinationLookupService != null) {
            Map<UUID, String> examNames = examinationLookupService.findExamNames(Set.of(request.getExamId()));
            examName = examNames.get(request.getExamId());
        }
        String shiftName = null;
        if (request.getShiftId() != null && examinationLookupService != null) {
            Map<String, String> shiftNames = examinationLookupService.findShiftNames(Set.of(request.getShiftId()));
            shiftName = shiftNames.get(request.getShiftId());
        }

        StringBuilder sb = new StringBuilder();
        if (Boolean.TRUE.equals(request.getIsPractice())) {
            sb.append("Practice - ");
        }
        if (examName != null && !examName.isBlank()) {
            sb.append(examName);
        } else {
            sb.append("Exam Paper");
        }
        if (shiftName != null && !shiftName.isBlank()) {
            sb.append(" (").append(shiftName).append(")");
        } else if (request.getShiftId() != null && !request.getShiftId().isBlank()) {
            sb.append(" [Shift: ").append(request.getShiftId()).append("]");
        }

        return sb.toString();
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
     * Computes the average difficulty score of the selected questions.
     * Weights: EASY = 1.0, MEDIUM = 2.0, HARD = 3.0.
     */
    double computeDifficultyScore(List<QuestionSummary> questions) {
        if (questions == null || questions.isEmpty()) {
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
        event.put("paperId", paper.getId() != null ? paper.getId().toString() : "");
        event.put("name", paper.getName() != null ? paper.getName() : "");
        event.put("examId", paper.getExamId().toString());
        event.put("shiftId", paper.getShiftId());
        event.put("status", paper.getStatus());
        event.put("isPractice", paper.isPractice());
        event.put("difficultyScore", paper.getDifficultyScore());
        event.put("timestamp", Instant.now().toString());

        eventPublisher.publish(PAPER_EVENTS_TOPIC, paper.getId() != null ? paper.getId().toString() : "", event);
        log.debug("Published paper generation event to topic={}, paperId={}",
                PAPER_EVENTS_TOPIC, paper.getId());
    }

    /**
     * Publishes an administrative notification alert when blueprint rules cannot be satisfied.
     */
    private void publishInsufficientQuestionsAlert(
            List<GapDetail> gapDetails,
            @Nullable UUID examId,
            @Nullable String shiftId,
            String tenantId) {
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("eventType", "BLUEPRINT_INSUFFICIENT_QUESTIONS_ALERT");
            event.put("recipientRole", "ADMIN");
            event.put("severity", "HIGH");
            event.put("examId", examId != null ? examId.toString() : "");
            event.put("shiftId", shiftId != null ? shiftId : "");
            event.put("tenantId", tenantId);
            event.put("gapCount", gapDetails.size());
            event.put("gapDetails", gapDetails);
            event.put("message", String.format(
                    "Blueprint generation deficit: %d rule(s) in Question Bank have insufficient approved questions.",
                    gapDetails.size()));
            event.put("timestamp", Instant.now().toString());

            String eventKey = examId != null ? examId.toString() : tenantId;
            eventPublisher.publish(NOTIFICATION_TOPIC, eventKey, event);
            log.info("Dispatched administrative alert to topic={} for {} question gap(s)",
                    NOTIFICATION_TOPIC, gapDetails.size());
        } catch (Exception ex) {
            log.error("Failed to publish insufficient questions alert notification: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Publishes an audit event for blueprint question deficit.
     */
    private void publishInsufficientQuestionsAuditEvent(
            List<GapDetail> gapDetails,
            @Nullable UUID examId,
            @Nullable String shiftId,
            String tenantId) {
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("eventType", "BLUEPRINT_INSUFFICIENT_QUESTIONS_AUDIT");
            event.put("examId", examId != null ? examId.toString() : "");
            event.put("shiftId", shiftId != null ? shiftId : "");
            event.put("tenantId", tenantId);
            event.put("gapCount", gapDetails.size());
            event.put("gapDetails", gapDetails);
            event.put("occurredAt", Instant.now().toString());

            String eventKey = examId != null ? examId.toString() : tenantId;
            eventPublisher.publish(AUDIT_TOPIC, eventKey, event);
            log.debug("Published blueprint gap audit event to topic={}", AUDIT_TOPIC);
        } catch (Exception ex) {
            log.error("Failed to publish insufficient questions audit event: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Publishes a paper audit event to the audit topic (fire-and-forget).
     */
    private void publishAuditEvent(String eventType, Paper paper, String tenantId) {
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("eventType", eventType);
            event.put("paperId", paper.getId() != null ? paper.getId().toString() : "");
            event.put("name", paper.getName() != null ? paper.getName() : "");
            event.put("examId", paper.getExamId().toString());
            event.put("shiftId", paper.getShiftId());
            event.put("isPractice", paper.isPractice());
            event.put("tenantId", tenantId);
            event.put("occurredAt", Instant.now().toString());

            eventPublisher.publish(AUDIT_TOPIC, paper.getId() != null ? paper.getId().toString() : "", event);
        } catch (Exception ex) {
            log.error("Error creating audit event [{}] for paper [{}]: {}",
                    eventType, paper.getId(), ex.getMessage());
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }
}
