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

package com.examplatform.papergenerator.service;

import com.examplatform.papergenerator.client.QuestionBankClient;
import com.examplatform.papergenerator.crypto.CanonicalPaperManifest;
import com.examplatform.papergenerator.crypto.MerkleTree;
import com.examplatform.papergenerator.domain.Blueprint;
import com.examplatform.papergenerator.domain.BlueprintSection;
import com.examplatform.papergenerator.domain.Paper;
import com.examplatform.papergenerator.domain.PaperQuestion;
import com.examplatform.papergenerator.domain.PaperSection;
import com.examplatform.papergenerator.dto.*;
import com.examplatform.papergenerator.dto.BlueprintRule;
import com.examplatform.papergenerator.exception.InsufficientQuestionsException;
import com.examplatform.papergenerator.repository.BlueprintRepository;
import com.examplatform.papergenerator.repository.PaperQuestionRepository;
import com.examplatform.papergenerator.repository.PaperRepository;
import com.examplatform.papergenerator.repository.PaperSectionRepository;
import com.examplatform.shared.constants.MessagingConstants;
import com.examplatform.shared.constants.QuestionStateConstants;
import com.examplatform.shared.constants.QuestionTaxonomyConstants;
import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.messaging.EventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.examplatform.shared.constants.MessagingConstants.AUDIT_EVENTS_TOPIC;

/**
 * Service responsible for blueprint-driven paper assembly and feasibility verification.
 * Selects questions satisfying subject/topic/subtopic/difficulty/cognitive ratios,
 * enforces reuse policies, builds structured PaperDocument sections,
 * calculates cryptographic Merkle tree root hashes, and alerts admins on question deficits.
 *
 * Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5, Issue #156
 */
@Slf4j
@Service
@Transactional
public class PaperAssemblyService {

    private static final String PAPER_EVENTS_TOPIC = "exam.paper.events";

    private static final String NOTIFICATION_TOPIC = MessagingConstants.NOTIFICATIONS_OUTBOUND_TOPIC;
    private static final String STATUS_DRAFT = QuestionStateConstants.DRAFT;

    private static final String REUSE_POLICY_NEVER = "NEVER";
    private static final String REUSE_POLICY_1_YEAR = "1_YEAR";
    private static final String REUSE_POLICY_2_YEARS = "2_YEARS";

    private final QuestionBankClient questionBankClient;
    private final PaperRepository paperRepository;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final ExaminationLookupService examinationLookupService;
    private final PaperSectionRepository paperSectionRepository;
    private final PaperQuestionRepository paperQuestionRepository;
    private final BlueprintRepository blueprintRepository;

    @Autowired
    public PaperAssemblyService(
            QuestionBankClient questionBankClient,
            PaperRepository paperRepository,
            EventPublisher eventPublisher,
            ObjectMapper objectMapper,
            @Nullable ExaminationLookupService examinationLookupService,
            @Autowired(required = false) PaperSectionRepository paperSectionRepository,
            @Autowired(required = false) PaperQuestionRepository paperQuestionRepository,
            @Autowired(required = false) BlueprintRepository blueprintRepository) {
        this.questionBankClient = questionBankClient;
        this.paperRepository = paperRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.examinationLookupService = examinationLookupService;
        this.paperSectionRepository = paperSectionRepository;
        this.paperQuestionRepository = paperQuestionRepository;
        this.blueprintRepository = blueprintRepository;
    }

    public PaperAssemblyService(
            QuestionBankClient questionBankClient,
            PaperRepository paperRepository,
            EventPublisher eventPublisher,
            ObjectMapper objectMapper,
            @Nullable ExaminationLookupService examinationLookupService) {
        this(questionBankClient, paperRepository, eventPublisher, objectMapper, examinationLookupService, null, null, null);
    }

    public PaperAssemblyService(
            QuestionBankClient questionBankClient,
            PaperRepository paperRepository,
            EventPublisher eventPublisher,
            ObjectMapper objectMapper) {
        this(questionBankClient, paperRepository, eventPublisher, objectMapper, null, null, null, null);
    }

    /**
     * Checks blueprint rules against current question bank availability without generating a paper.
     * Evaluates question reuse policies, computes surpluses/deficits, and optionally alerts admins on deficit.
     * Granularity is evaluated at subtopic level when provided.
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

            List<QuestionSummary> candidates = (rule.getSubjectId() != null)
                    ? questionBankClient.findAvailableQuestions(
                            rule.getSubjectId(), rule.getTopicId(), rule.getSubtopicId(),
                            rule.getDifficulty(), rule.getCognitiveLevel(), tenantId)
                    : questionBankClient.findAvailableQuestions(
                            rule.getSubject(), rule.getTopic(),
                            rule.getDifficulty(), rule.getCognitiveLevel(), tenantId);

            List<QuestionSummary> eligible = candidates.stream()
                    .filter(this::isEligibleForReuse)
                    .toList();

            int available = eligible.size();
            totalAvailable += available;

            if (available < needed) {
                int deficit = needed - available;
                log.warn("Blueprint rule deficit detected: subjectId={}, topicId={}, subtopicId={}, needed={}, available={}, deficit={}",
                        rule.getSubjectId(), rule.getTopicId(), rule.getSubtopicId(), needed, available, deficit);

                GapDetail gap = GapDetail.builder()
                        .subjectId(rule.getSubjectId())
                        .topicId(rule.getTopicId())
                        .subtopicId(rule.getSubtopicId())
                        .subject(rule.getSubject())
                        .topic(rule.getTopic())
                        .subtopic(rule.getSubtopic())
                        .difficulty(rule.getDifficulty())
                        .needed(needed)
                        .available(available)
                        .build();
                gapDetails.add(gap);

                ruleDetails.add(RuleFeasibilityDetail.builder()
                        .subjectId(rule.getSubjectId())
                        .topicId(rule.getTopicId())
                        .subtopicId(rule.getSubtopicId())
                        .subject(rule.getSubject())
                        .topic(rule.getTopic())
                        .subtopic(rule.getSubtopic())
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
                        .subjectId(rule.getSubjectId())
                        .topicId(rule.getTopicId())
                        .subtopicId(rule.getSubtopicId())
                        .subject(rule.getSubject())
                        .topic(rule.getTopic())
                        .subtopic(rule.getSubtopic())
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
     * ID-based sufficiency check: verifies whether all rules of a saved blueprint can be satisfied,
     * scaled by shift count and buffer.
     */
    @Transactional(readOnly = true)
    public BlueprintFeasibilityResponse checkBlueprintSufficiencyById(
            UUID blueprintId,
            @Nullable UUID examId,
            @Nullable Integer shifts,
            String tenantId,
            boolean notifyAdmin) {

        if (blueprintRepository == null) {
            return checkBlueprintSufficiency(List.of(), examId, null, tenantId, notifyAdmin);
        }

        Optional<Blueprint> bpOpt = blueprintRepository.findVisibleById(blueprintId, tenantId);
        if (bpOpt.isEmpty()) {
            throw new EntityNotFoundException("Blueprint not found: " + blueprintId);
        }

        Blueprint bp = bpOpt.get();
        int shiftMultiplier = (shifts != null && shifts > 1) ? shifts : 1;
        List<BlueprintRule> rules = new ArrayList<>();

        if (bp.getSections() != null) {
            for (BlueprintSection s : bp.getSections()) {
                if (s.getRules() != null) {
                    for (com.examplatform.papergenerator.domain.BlueprintRule r : s.getRules()) {
                        rules.add(BlueprintRule.builder()
                                .id(r.getId())
                                .subjectId(r.getSubjectId())
                                .topicId(r.getTopicId())
                                .subtopicId(r.getSubtopicId())
                                .subject(s.getName())
                                .difficulty(r.getDifficulty())
                                .cognitiveLevel(r.getCognitiveLevel())
                                .questionCount(r.getQuestionCount() * shiftMultiplier)
                                .marksOverride(r.getMarksOverride())
                                .orderIndex(r.getOrderIndex())
                                .build());
                    }
                }
            }
        }

        return checkBlueprintSufficiency(rules, examId, null, tenantId, notifyAdmin);
    }

    /**
     * Generates a paper from the given blueprint request, preserving relational sections
     * and recording question positions and subtopic snapshots.
     */
    public Paper generatePaper(PaperGenerationRequest request, UUID generatedBy, String tenantId) {
        log.info("Starting paper generation for examId={}, shiftId={}, isPractice={}, tenantId={}",
                request.getExamId(), request.getShiftId(), request.getIsPractice(), tenantId);

        List<UUID> selectedQuestionIds = new ArrayList<>();
        List<QuestionSummary> allSelectedQuestions = new ArrayList<>();
        List<GapDetail> gapDetails = new ArrayList<>();
        List<PaperSection> paperSections = new ArrayList<>();

        UUID blueprintId = request.getBlueprintId();
        int blueprintVersion = 1;
        int globalPos = 1;

        if (blueprintId != null && blueprintRepository != null) {
            Optional<Blueprint> bpOpt = blueprintRepository.findVisibleById(blueprintId, tenantId);
            if (bpOpt.isPresent()) {
                Blueprint bp = bpOpt.get();
                blueprintVersion = bp.getBlueprintVersion();

                if (bp.getSections() != null) {
                    for (BlueprintSection s : bp.getSections()) {
                        PaperSection ps = PaperSection.builder()
                                .blueprintSectionId(s.getId())
                                .name(s.getName())
                                .orderIndex(s.getOrderIndex())
                                .questions(new ArrayList<>())
                                .build();
                        ps.setTenantId(tenantId);

                        if (s.getRules() != null) {
                            for (com.examplatform.papergenerator.domain.BlueprintRule rule : s.getRules()) {
                                List<QuestionSummary> candidates = questionBankClient.findAvailableQuestions(
                                        rule.getSubjectId(), rule.getTopicId(), rule.getSubtopicId(),
                                        rule.getDifficulty(), rule.getCognitiveLevel(), tenantId);

                                List<QuestionSummary> eligible = candidates.stream()
                                        .filter(this::isEligibleForReuse)
                                        .toList();

                                List<QuestionSummary> selected = eligible.stream()
                                        .limit(rule.getQuestionCount())
                                        .toList();

                                if (selected.size() < rule.getQuestionCount()) {
                                    gapDetails.add(GapDetail.builder()
                                            .subjectId(rule.getSubjectId())
                                            .topicId(rule.getTopicId())
                                            .subtopicId(rule.getSubtopicId())
                                            .difficulty(rule.getDifficulty())
                                            .needed(rule.getQuestionCount())
                                            .available(selected.size())
                                            .build());
                                }

                                for (QuestionSummary q : selected) {
                                    PaperQuestion pq = PaperQuestion.builder()
                                            .paperSection(ps)
                                            .questionId(q.getQuestionId())
                                            .position(globalPos++)
                                            .subtopicId(q.getSubtopicId())
                                            .build();
                                    pq.setTenantId(tenantId);
                                    ps.addQuestion(pq);
                                }

                                selectedQuestionIds.addAll(selected.stream().map(QuestionSummary::getQuestionId).toList());
                                allSelectedQuestions.addAll(selected);
                            }
                        }
                        paperSections.add(ps);
                    }
                }
            }
        }

        // Fallback: legacy list of blueprint rules if blueprintId not resolved
        if (paperSections.isEmpty() && request.getBlueprintRules() != null) {
            PaperSection defaultSection = PaperSection.builder()
                    .name("Default Section")
                    .orderIndex(0)
                    .questions(new ArrayList<>())
                    .build();
            defaultSection.setTenantId(tenantId);

            for (BlueprintRule rule : request.getBlueprintRules()) {
                List<QuestionSummary> candidates = (rule.getSubjectId() != null)
                        ? questionBankClient.findAvailableQuestions(
                                rule.getSubjectId(), rule.getTopicId(), rule.getSubtopicId(),
                                rule.getDifficulty(), rule.getCognitiveLevel(), tenantId)
                        : questionBankClient.findAvailableQuestions(
                                rule.getSubject(), rule.getTopic(),
                                rule.getDifficulty(), rule.getCognitiveLevel(), tenantId);

                List<QuestionSummary> eligible = candidates.stream()
                        .filter(this::isEligibleForReuse)
                        .toList();

                List<QuestionSummary> selected = eligible.stream()
                        .limit(rule.getQuestionCount())
                        .toList();

                if (selected.size() < rule.getQuestionCount()) {
                    log.warn("Insufficient questions for rule: subject={}, topic={}, difficulty={}, needed={}, available={}",
                            rule.getSubject(), rule.getTopic(), rule.getDifficulty(),
                            rule.getQuestionCount(), selected.size());
                    gapDetails.add(GapDetail.builder()
                            .subjectId(rule.getSubjectId())
                            .topicId(rule.getTopicId())
                            .subtopicId(rule.getSubtopicId())
                            .subject(rule.getSubject())
                            .topic(rule.getTopic())
                            .subtopic(rule.getSubtopic())
                            .difficulty(rule.getDifficulty())
                            .needed(rule.getQuestionCount())
                            .available(selected.size())
                            .build());
                }

                for (QuestionSummary q : selected) {
                    PaperQuestion pq = PaperQuestion.builder()
                            .paperSection(defaultSection)
                            .questionId(q.getQuestionId())
                            .position(globalPos++)
                            .subtopicId(q.getSubtopicId())
                            .build();
                    pq.setTenantId(tenantId);
                    defaultSection.addQuestion(pq);
                }

                selectedQuestionIds.addAll(selected.stream()
                        .map(QuestionSummary::getQuestionId)
                        .toList());
                allSelectedQuestions.addAll(selected);
            }
            paperSections.add(defaultSection);
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
                .filter(q -> q.getTopic() != null)
                .collect(Collectors.groupingBy(QuestionSummary::getTopic, Collectors.counting()));

        // Group questions that belong to passages
        List<QuestionGroup> questionGroups = new ArrayList<>();
        Map<UUID, List<QuestionSummary>> passageMap = new LinkedHashMap<>();
        for (QuestionSummary q : allSelectedQuestions) {
            if (q.getPassageId() != null) {
                passageMap.computeIfAbsent(q.getPassageId(), k -> new ArrayList<>()).add(q);
            }
        }
        for (Map.Entry<UUID, List<QuestionSummary>> entry : passageMap.entrySet()) {
            List<UUID> orderedSubQIds = entry.getValue().stream()
                    .sorted((a, b) -> {
                        int idxA = a.getPassageOrderIndex() != null ? a.getPassageOrderIndex() : 0;
                        int idxB = b.getPassageOrderIndex() != null ? b.getPassageOrderIndex() : 0;
                        return Integer.compare(idxA, idxB);
                    })
                    .map(QuestionSummary::getQuestionId)
                    .toList();
            questionGroups.add(new QuestionGroup(entry.getKey(), orderedSubQIds));
        }

        // Build structured paper sections for PaperDocument
        List<PaperDocument.PaperSectionDocument> sectionDocs = new ArrayList<>();
        for (PaperSection ps : paperSections) {
            List<PaperDocument.PaperQuestionDocument> qDocs = (ps.getQuestions() != null)
                    ? ps.getQuestions().stream()
                            .map(pq -> PaperDocument.PaperQuestionDocument.builder()
                                    .questionId(pq.getQuestionId())
                                    .position(pq.getPosition())
                                    .subtopicId(pq.getSubtopicId())
                                    .build())
                            .toList()
                    : List.of();

            sectionDocs.add(PaperDocument.PaperSectionDocument.builder()
                    .sectionId(ps.getId())
                    .blueprintSectionId(ps.getBlueprintSectionId())
                    .examinationSectionId(ps.getExaminationSectionId())
                    .name(ps.getName())
                    .orderIndex(ps.getOrderIndex())
                    .questions(qDocs)
                    .build());
        }

        // Build paper definition and topic distribution JSON
        Map<String, Object> defMap = new LinkedHashMap<>();
        defMap.put("questionIds", selectedQuestionIds);
        defMap.put("sections", sectionDocs);
        if (!questionGroups.isEmpty()) {
            defMap.put("questionGroups", questionGroups);
        }

        String paperDefinitionJson = toJson(defMap);
        String topicDistributionJson = toJson(topicDistribution);

        // Resolve meaningful paper name
        String paperName = resolvePaperName(request);

        // Compute Cryptographic Merkle Root & Canonical Manifest Digest (Issue #156)
        String variant = "SET-A";
        Map<String, Integer> topicDistInt = topicDistribution.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().intValue()));
        CanonicalPaperManifest.ManifestData manifestData = CanonicalPaperManifest.ManifestData.builder()
                .schemaVersion("1.0")
                .examId(request.getExamId())
                .shiftId(request.getShiftId())
                .variant(variant)
                .questionIds(selectedQuestionIds)
                .topicDistribution(topicDistInt)
                .difficultyScore(difficultyScore)
                .isPractice(Boolean.TRUE.equals(request.getIsPractice()))
                .generatedAt(Instant.now().toString())
                .build();
        String manifestDigest = CanonicalPaperManifest.computeManifestDigest(manifestData);
        List<String> leafPayloads = CanonicalPaperManifest.buildQuestionLeafPayloads(selectedQuestionIds, request.getExamId(), variant);
        MerkleTree merkleTree = new MerkleTree(leafPayloads);
        String paperRootHash = merkleTree.getRootHash();

        // Create Paper entity in DRAFT status
        Paper paper = Paper.builder()
                .name(paperName)
                .examId(request.getExamId())
                .blueprintId(blueprintId)
                .blueprintVersion(blueprintVersion)
                .shiftId(request.getShiftId())
                .variant(variant)
                .paperRootHash(paperRootHash)
                .manifestDigest(manifestDigest)
                .status(STATUS_DRAFT)
                .isPractice(Boolean.TRUE.equals(request.getIsPractice()))
                .paperDefinitionJson(paperDefinitionJson)
                .difficultyScore(difficultyScore)
                .topicDistributionJson(topicDistributionJson)
                .generatedBy(generatedBy)
                .build();
        paper.setTenantId(tenantId);

        Paper savedPaper = paperRepository.save(paper);

        // Persist relational sections and questions
        for (PaperSection ps : paperSections) {
            ps.setPaper(savedPaper);
        }
        if (paperSectionRepository != null && !paperSections.isEmpty()) {
            paperSectionRepository.saveAll(paperSections);
            if (paperQuestionRepository != null) {
                List<PaperQuestion> allPQs = paperSections.stream()
                        .flatMap(ps -> ps.getQuestions().stream())
                        .toList();
                paperQuestionRepository.saveAll(allPQs);
            }
        }
        savedPaper.setSections(paperSections);

        // Publish async job result to Kafka
        publishPaperEvent(savedPaper);

        // Publish PAPER_GENERATED audit event (fire-and-forget)
        publishAuditEvent("PAPER_GENERATED", savedPaper, tenantId);

        log.info("Paper generated successfully: paperId={}, name='{}', isPractice={}, questionCount={}, difficultyScore={}, rootHash={}",
                savedPaper.getId(), savedPaper.getName(), savedPaper.isPractice(), selectedQuestionIds.size(), difficultyScore, paperRootHash);

        return savedPaper;
    }

    @Transactional(readOnly = true)
    public Page<Paper> listPapers(String tenantId, UUID examId, String status, Pageable pageable) {
        return paperRepository.findPapers(tenantId, examId, status, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Paper> getPaperById(UUID paperId, String tenantId) {
        return paperRepository.findByIdAndTenantId(paperId, tenantId)
                .or(() -> paperRepository.findById(paperId));
    }

    @Transactional(readOnly = true)
    public List<Paper> findPracticePapers(UUID examId, String tenantId) {
        return paperRepository.findPracticePapersByExamId(examId, tenantId);
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

        return formatPaperTitle(Boolean.TRUE.equals(request.getIsPractice()), examName, shiftName, request.getShiftId());
    }

    public static String formatPaperTitle(boolean isPractice, String examName, String shiftName, String shiftId) {
        StringBuilder sb = new StringBuilder();
        if (isPractice) {
            sb.append("Practice - ");
        }
        if (examName != null && !examName.isBlank()) {
            sb.append(examName);
        } else {
            sb.append("Exam Paper");
        }
        if (shiftName != null && !shiftName.isBlank()) {
            sb.append(" (").append(shiftName).append(")");
        } else if (shiftId != null && !shiftId.isBlank()) {
            sb.append(" [Shift: ").append(shiftId).append("]");
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
            case REUSE_POLICY_1_YEAR -> {
                if (question.getLastUsedAt() == null) {
                    yield true;
                }
                Instant oneYearAgo = Instant.now().minus(365, ChronoUnit.DAYS);
                yield question.getLastUsedAt().isBefore(oneYearAgo);
            }
            case REUSE_POLICY_2_YEARS -> {
                if (question.getLastUsedAt() == null) {
                    yield true;
                }
                Instant twoYearsAgo = Instant.now().minus(730, ChronoUnit.DAYS);
                yield question.getLastUsedAt().isBefore(twoYearsAgo);
            }
            default -> true;
        };
    }

    /**
     * Computes paper difficulty score as average of question difficulty weights.
     */
    double computeDifficultyScore(List<QuestionSummary> questions) {
        if (questions == null || questions.isEmpty()) {
            return 0.0;
        }

        double totalWeight = questions.stream()
                .mapToDouble(this::getDifficultyWeight)
                .sum();

        return totalWeight / questions.size();
    }

    private double getDifficultyWeight(QuestionSummary question) {
        if (question.getDifficulty() == null) {
            return 2.0;
        }

        return switch (question.getDifficulty().toUpperCase()) {
            case QuestionTaxonomyConstants.Difficulty.EASY -> 1.0;
            case QuestionTaxonomyConstants.Difficulty.HARD -> 3.0;
            default -> 2.0;
        };
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON: {}", e.getMessage());
            throw new RuntimeException("JSON serialization error", e);
        }
    }

    private void publishPaperEvent(Paper paper) {
        try {
            eventPublisher.publish(PAPER_EVENTS_TOPIC, paper.getId().toString(), paper);
        } catch (Exception e) {
            log.error("Unexpected error publishing paper event for paper {}: {}", paper.getId(), e.getMessage());
        }
    }

    private void publishAuditEvent(String eventType, Paper paper, String tenantId) {
        try {
            Map<String, Object> event = Map.of(
                SharedConstants.EVENT_TYPE, eventType,
                    "paperId", paper.getId().toString(),
                    "examId", paper.getExamId().toString(),
                    "shiftId", paper.getShiftId(),
                SharedConstants.TENANT_ID, tenantId,
                SharedConstants.OCCURRED_AT, Instant.now().toString()
            );
            eventPublisher.publish(AUDIT_EVENTS_TOPIC, paper.getId().toString(), event);
        } catch (Exception e) {
            log.error("Unexpected error publishing audit event {} for paper {}: {}",
                    eventType, paper.getId(), e.getMessage());
        }
    }

    private void publishInsufficientQuestionsAlert(
            List<GapDetail> gaps, @Nullable UUID examId, @Nullable String shiftId, String tenantId) {
        publishDeficitPayload(NOTIFICATION_TOPIC, "notificationType", "BLUEPRINT_DEFICIT_ALERT", gaps, examId, shiftId, tenantId);
    }

    private void publishInsufficientQuestionsAuditEvent(
            List<GapDetail> gaps, @Nullable UUID examId, @Nullable String shiftId, String tenantId) {
        publishDeficitPayload(AUDIT_EVENTS_TOPIC, SharedConstants.EVENT_TYPE, "BLUEPRINT_DEFICIT_DETECTED", gaps, examId, shiftId, tenantId);
    }

    private void publishDeficitPayload(
            String topic, String typeKey, String typeValue,
            List<GapDetail> gaps, @Nullable UUID examId, @Nullable String shiftId, String tenantId) {
        try {
            Map<String, Object> payload = Map.of(
                    typeKey, typeValue,
                    "examId", examId != null ? examId.toString() : "",
                    "shiftId", shiftId != null ? shiftId : "",
                SharedConstants.TENANT_ID, tenantId,
                    "deficitRuleCount", gaps.size(),
                    "gapDetails", gaps,
                SharedConstants.OCCURRED_AT, Instant.now().toString()
            );
            eventPublisher.publish(topic, examId != null ? examId.toString() : "GLOBAL", payload);
            log.info("Dispatched {} to {} for examId={}, shiftId={}, deficitCount={}",
                    typeValue, topic, examId, shiftId, gaps.size());
        } catch (Exception e) {
            log.error("Failed to publish {} event to {}: {}", typeValue, topic, e.getMessage());
        }
    }

    public List<UUID> extractQuestionIds(Paper paper) {
        return extractQuestionIds(paper, this.objectMapper);
    }

    public static List<UUID> extractQuestionIds(Paper paper, ObjectMapper objectMapper) {
        if (paper == null || paper.getPaperDefinitionJson() == null || paper.getPaperDefinitionJson().isBlank()) {
            return List.of();
        }
        List<UUID> questionIds = new ArrayList<>();
        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(paper.getPaperDefinitionJson());
            com.fasterxml.jackson.databind.JsonNode qIdsNode = root.get("questionIds");
            if (qIdsNode != null && qIdsNode.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode qNode : qIdsNode) {
                    try {
                        questionIds.add(UUID.fromString(qNode.asText()));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse paperDefinitionJson for paper {}: {}", paper.getId(), e.getMessage());
        }
        return questionIds;
    }
}
