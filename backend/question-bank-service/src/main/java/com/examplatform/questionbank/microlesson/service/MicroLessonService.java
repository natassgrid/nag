// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.service;

import com.examplatform.questionbank.microlesson.ai.KaTexValidator;
import com.examplatform.questionbank.microlesson.ai.MicroLessonGeneratorService;
import com.examplatform.questionbank.microlesson.ai.agent.MicroLessonBlueprintAgent;
import com.examplatform.questionbank.microlesson.domain.MicroLessonStatus;
import com.examplatform.questionbank.microlesson.domain.SubtopicMicroLesson;
import com.examplatform.questionbank.microlesson.dto.*;
import com.examplatform.questionbank.microlesson.repository.SubtopicMicroLessonRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.examplatform.questionbank.domain.Subject;
import com.examplatform.questionbank.domain.Subtopic;
import com.examplatform.questionbank.domain.Topic;
import com.examplatform.questionbank.repository.SubjectRepository;
import com.examplatform.questionbank.repository.SubtopicRepository;
import com.examplatform.questionbank.repository.TopicRepository;
import com.examplatform.questionbank.microlesson.translation.service.MicroLessonTranslationWorkflowService;
import com.examplatform.shared.tenant.TenantContext;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MicroLessonService {

    private final SubtopicMicroLessonRepository repository;
    private final MicroLessonGeneratorService generatorService;
    private final MicroLessonCacheService cacheService;
    private final KaTexValidator kaTexValidator;
    private final ObjectMapper objectMapper;
    private final SubtopicRepository subtopicRepository;
    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;
    private final MicroLessonTranslationWorkflowService translationWorkflowService;
    private final com.examplatform.questionbank.microlesson.repository.LessonEffectivenessRepository effectivenessRepository;
    private final MicroLessonBlueprintAgent blueprintAgent = new MicroLessonBlueprintAgent();

    public MicroLessonService(
            SubtopicMicroLessonRepository repository,
            MicroLessonGeneratorService generatorService,
            MicroLessonCacheService cacheService,
            KaTexValidator kaTexValidator,
            ObjectMapper objectMapper) {
        this(repository, generatorService, cacheService, kaTexValidator, objectMapper, null, null, null, null, null);
    }

    public MicroLessonService(
            SubtopicMicroLessonRepository repository,
            MicroLessonGeneratorService generatorService,
            MicroLessonCacheService cacheService,
            KaTexValidator kaTexValidator,
            ObjectMapper objectMapper,
            SubtopicRepository subtopicRepository,
            TopicRepository topicRepository,
            SubjectRepository subjectRepository) {
        this(repository, generatorService, cacheService, kaTexValidator, objectMapper, subtopicRepository, topicRepository, subjectRepository, null, null);
    }

    @Autowired
    public MicroLessonService(
            SubtopicMicroLessonRepository repository,
            MicroLessonGeneratorService generatorService,
            MicroLessonCacheService cacheService,
            KaTexValidator kaTexValidator,
            ObjectMapper objectMapper,
            @Autowired(required = false) SubtopicRepository subtopicRepository,
            @Autowired(required = false) TopicRepository topicRepository,
            @Autowired(required = false) SubjectRepository subjectRepository,
            @Autowired(required = false) MicroLessonTranslationWorkflowService translationWorkflowService,
            @Autowired(required = false) com.examplatform.questionbank.microlesson.repository.LessonEffectivenessRepository effectivenessRepository) {
        this.repository = repository;
        this.generatorService = generatorService;
        this.cacheService = cacheService;
        this.kaTexValidator = kaTexValidator;
        this.objectMapper = objectMapper;
        this.subtopicRepository = subtopicRepository;
        this.topicRepository = topicRepository;
        this.subjectRepository = subjectRepository;
        this.translationWorkflowService = translationWorkflowService;
        this.effectivenessRepository = effectivenessRepository;
    }

    @Transactional(readOnly = true)
    public Page<MicroLessonResponse> listForReview(String status, Long subjectId, Pageable pageable) {
        return listForReview(status, subjectId, null, null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<MicroLessonResponse> listForReview(
            String status,
            Long subjectId,
            String subject,
            String topic,
            String targetExam,
            String search,
            Pageable pageable) {

        Page<SubtopicMicroLesson> result;
        boolean hasExtendedFilters = (subject != null && !subject.isBlank() && !"ALL".equalsIgnoreCase(subject))
                || (topic != null && !topic.isBlank() && !"ALL".equalsIgnoreCase(topic))
                || (targetExam != null && !targetExam.isBlank() && !"ALL".equalsIgnoreCase(targetExam))
                || (search != null && !search.isBlank())
                || (status != null && "ALL".equalsIgnoreCase(status.trim()));

        if (!hasExtendedFilters) {
            if (status != null && !status.isBlank() && subjectId != null) {
                result = repository.findByStatusAndSubjectId(status.toUpperCase(), subjectId, pageable);
            } else if (status != null && !status.isBlank()) {
                result = repository.findByStatus(status.toUpperCase(), pageable);
            } else if (subjectId != null) {
                result = repository.findBySubjectId(subjectId, pageable);
            } else {
                result = repository.findByStatus(MicroLessonStatus.AI_GENERATED.name(), pageable);
            }
        } else {
            Specification<SubtopicMicroLesson> spec = buildSpecification(status, subjectId, subject, topic, targetExam, search);
            result = repository.findAll(spec, pageable);
        }

        return toResponsePage(result);
    }

    private Specification<SubtopicMicroLesson> buildSpecification(
            String status,
            Long subjectId,
            String subject,
            String topic,
            String targetExam,
            String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }

            Long resolvedSubjectId = subjectId;
            if (resolvedSubjectId != null) {
                predicates.add(cb.equal(root.get("subjectId"), resolvedSubjectId));
            } else if (subject != null && !subject.isBlank() && !"ALL".equalsIgnoreCase(subject.trim())) {
                List<Long> matchedSubjectIds = resolveSubjectIds(subject.trim());
                if (!matchedSubjectIds.isEmpty()) {
                    predicates.add(root.get("subjectId").in(matchedSubjectIds));
                    if (matchedSubjectIds.size() == 1) {
                        resolvedSubjectId = matchedSubjectIds.get(0);
                    }
                } else {
                    predicates.add(cb.disjunction());
                }
            }

            if (topic != null && !topic.isBlank() && !"ALL".equalsIgnoreCase(topic.trim())) {
                List<Long> matchedTopicIds = resolveTopicIds(topic.trim(), resolvedSubjectId);
                if (!matchedTopicIds.isEmpty()) {
                    predicates.add(root.get("topicId").in(matchedTopicIds));
                } else {
                    predicates.add(cb.disjunction());
                }
            }

            if (search != null && !search.isBlank()) {
                String term = "%" + search.trim().toLowerCase() + "%";
                List<Predicate> searchOr = new ArrayList<>();
                searchOr.add(cb.like(cb.lower(root.get("coreConcept")), term));
                searchOr.add(cb.like(cb.lower(root.get("trap")), term));

                List<Long> subtopicIds = resolveSubtopicIdsForSearch(search.trim());
                if (!subtopicIds.isEmpty()) {
                    searchOr.add(root.get("subtopicId").in(subtopicIds));
                }

                List<Long> topicIds = resolveTopicIdsForSearch(search.trim());
                if (!topicIds.isEmpty()) {
                    searchOr.add(root.get("topicId").in(topicIds));
                }

                List<Long> subjectIds = resolveSubjectIdsForSearch(search.trim());
                if (!subjectIds.isEmpty()) {
                    searchOr.add(root.get("subjectId").in(subjectIds));
                }

                predicates.add(cb.or(searchOr.toArray(new Predicate[0])));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private List<Long> resolveSubjectIds(String subjectStr) {
        if (subjectStr == null || subjectStr.isBlank()) {
            return Collections.emptyList();
        }
        if (subjectStr.matches("\\d+")) {
            return List.of(Long.parseLong(subjectStr));
        }
        if (subjectRepository == null) {
            return Collections.emptyList();
        }
        try {
            Optional<Subject> exact = subjectRepository.findByNameAndTenantIdIsNull(subjectStr.trim());
            if (exact.isPresent()) {
                return List.of(exact.get().getId());
            }
        } catch (Exception ignored) {}
        try {
            List<Subject> byIgnoreCase = subjectRepository.findByNameIgnoreCaseAndTenantIdIsNull(subjectStr.trim());
            if (!byIgnoreCase.isEmpty()) {
                return byIgnoreCase.stream().map(Subject::getId).filter(Objects::nonNull).toList();
            }
        } catch (Exception ignored) {}
        try {
            return subjectRepository.findAll().stream()
                    .filter(s -> s.getName() != null && s.getName().equalsIgnoreCase(subjectStr.trim()))
                    .map(Subject::getId)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private List<Long> resolveTopicIds(String topicStr, Long subjectId) {
        if (topicStr == null || topicStr.isBlank()) {
            return Collections.emptyList();
        }
        if (topicStr.matches("\\d+")) {
            return List.of(Long.parseLong(topicStr));
        }
        if (topicRepository == null) {
            return Collections.emptyList();
        }
        try {
            if (subjectId != null) {
                Optional<Topic> exact = topicRepository.findByNameAndSubjectIdAndTenantIdIsNull(topicStr.trim(), subjectId);
                if (exact.isPresent()) {
                    return List.of(exact.get().getId());
                }
            }
        } catch (Exception ignored) {}
        try {
            List<Topic> byIgnoreCase = topicRepository.findByNameIgnoreCaseAndTenantIdIsNull(topicStr.trim());
            if (!byIgnoreCase.isEmpty()) {
                return byIgnoreCase.stream()
                        .filter(t -> subjectId == null || subjectId.equals(t.getSubjectId()))
                        .map(Topic::getId)
                        .filter(Objects::nonNull)
                        .toList();
            }
        } catch (Exception ignored) {}
        try {
            return topicRepository.findAll().stream()
                    .filter(t -> t.getName() != null && t.getName().equalsIgnoreCase(topicStr.trim()))
                    .filter(t -> subjectId == null || subjectId.equals(t.getSubjectId()))
                    .map(Topic::getId)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private List<Long> resolveSubtopicIdsForSearch(String term) {
        if (subtopicRepository == null || term == null || term.isBlank()) {
            return Collections.emptyList();
        }
        try {
            List<Subtopic> results = subtopicRepository.findByNameContainingIgnoreCase(term);
            if (results != null && !results.isEmpty()) {
                return results.stream().map(Subtopic::getId).filter(Objects::nonNull).toList();
            }
        } catch (Exception ignored) {}
        try {
            return subtopicRepository.findAll().stream()
                    .filter(st -> st.getName() != null && st.getName().toLowerCase().contains(term.toLowerCase()))
                    .map(Subtopic::getId)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private List<Long> resolveTopicIdsForSearch(String term) {
        if (topicRepository == null || term == null || term.isBlank()) {
            return Collections.emptyList();
        }
        try {
            List<Topic> results = topicRepository.findByNameContainingIgnoreCase(term);
            if (results != null && !results.isEmpty()) {
                return results.stream().map(Topic::getId).filter(Objects::nonNull).toList();
            }
        } catch (Exception ignored) {}
        try {
            return topicRepository.findAll().stream()
                    .filter(t -> t.getName() != null && t.getName().toLowerCase().contains(term.toLowerCase()))
                    .map(Topic::getId)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private List<Long> resolveSubjectIdsForSearch(String term) {
        if (subjectRepository == null || term == null || term.isBlank()) {
            return Collections.emptyList();
        }
        try {
            List<Subject> results = subjectRepository.findByNameContainingIgnoreCase(term);
            if (results != null && !results.isEmpty()) {
                return results.stream().map(Subject::getId).filter(Objects::nonNull).toList();
            }
        } catch (Exception ignored) {}
        try {
            return subjectRepository.findAll().stream()
                    .filter(s -> s.getName() != null && s.getName().toLowerCase().contains(term.toLowerCase()))
                    .map(Subject::getId)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    @Transactional(readOnly = true)
    public MicroLessonStatsResponse getStats() {
        long total = repository.count();
        long aiGenerated = repository.countByStatus(MicroLessonStatus.AI_GENERATED.name());
        long inReview = repository.countByStatus(MicroLessonStatus.IN_REVIEW.name());
        long approved = repository.countByStatus(MicroLessonStatus.APPROVED.name());
        long published = repository.countByStatus(MicroLessonStatus.PUBLISHED.name());
        long rejected = repository.countByStatus(MicroLessonStatus.REJECTED.name());
        return new MicroLessonStatsResponse(total, aiGenerated, inReview, approved, published, rejected);
    }

    @Transactional(readOnly = true)
    public MicroLessonFilterOptionsResponse getFilterOptions(String subject) {
        List<String> subjects = (subjectRepository != null)
                ? subjectRepository.findByTenantIdIsNullOrderByDisplayOrderAscNameAsc().stream().map(Subject::getName).toList()
                : List.of();
        List<String> topics = List.of();
        if (subject != null && !subject.isBlank() && !"ALL".equalsIgnoreCase(subject.trim()) && subjectRepository != null && topicRepository != null) {
            topics = subjectRepository.findByNameAndTenantIdIsNull(subject.trim())
                    .map(s -> topicRepository.findBySubjectIdAndTenantIdIsNullOrderByDisplayOrderAscNameAsc(s.getId())
                            .stream().map(Topic::getName).toList())
                    .orElse(List.of());
        } else if (topicRepository != null) {
            try {
                topics = topicRepository.findByTenantIdIsNullOrderByDisplayOrderAscNameAsc()
                        .stream().map(Topic::getName).distinct().toList();
            } catch (Exception ignored) {
                topics = List.of();
            }
        }
        List<String> statuses = List.of("ALL", "AI_GENERATED", "IN_REVIEW", "APPROVED", "PUBLISHED", "REJECTED");
        return new MicroLessonFilterOptionsResponse(subjects, topics, statuses);
    }

    @Transactional(readOnly = true)
    public MicroLessonResponse getMicroLessonById(UUID id) {
        SubtopicMicroLesson lesson = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Micro-lesson not found"));
        return toResponse(lesson);
    }

    @Transactional
    public MicroLessonResponse createManualMicroLesson(CreateMicroLessonRequest request, UUID authorId) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request payload cannot be null");
        }

        ResolvedTaxonomy taxonomy = resolveTaxonomyForCreate(
                request.subjectId(), request.subject(),
                request.topicId(), request.topic(),
                request.subtopicId(), request.subtopic()
        );

        String exam = (request.targetExam() != null && !request.targetExam().isBlank())
                ? request.targetExam().trim()
                : "GENERAL";

        MicroLessonStructuredContent nested = request.content();
        String coreConcept = firstNonBlank(request.coreConcept(), nested != null ? nested.coreConcept() : null);
        if (coreConcept == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Core concept cannot be empty");
        }

        TrapAlertDto structuredTrap = request.trapAlert() != null
                ? request.trapAlert()
                : (nested != null ? nested.trapAlert() : null);
        String trapText = resolveTrapText(request.trap(), structuredTrap);
        if (trapText == null || trapText.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trap description cannot be empty");
        }

        String lessonType = (request.lessonType() != null && !request.lessonType().isBlank())
                ? request.lessonType().trim() : "CONCEPT";
        boolean formulaRequired = blueprintAgent.isFormulaSheetRequired(
                taxonomy.subject(), taxonomy.topic(), taxonomy.subtopic(), lessonType);

        List<FormulaEntryDto> structuredFormulas = request.formulaCheatSheet() != null && !request.formulaCheatSheet().isEmpty()
                ? request.formulaCheatSheet()
                : (nested != null && nested.formulaCheatSheet() != null ? nested.formulaCheatSheet() : null);
        List<String> cheatSheetStrings = resolveCheatSheetStrings(request.cheatSheet(), structuredFormulas);
        if (formulaRequired && cheatSheetStrings.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cheat sheet must contain at least 1 formula or framework for Math, Science, or Numerical topics"
            );
        }

        List<String> takeaways = request.keyTakeaways() != null && !request.keyTakeaways().isEmpty()
                ? request.keyTakeaways()
                : (nested != null ? nested.keyTakeaways() : null);
        List<QuickCheckItemDto> quickChecks = request.quickCheckItems() != null && !request.quickCheckItems().isEmpty()
                ? request.quickCheckItems()
                : (nested != null ? nested.quickCheckItems() : null);

        StepByStepBreakdownDto breakdown = buildNormalizedBreakdown(
                request.stepByStepBreakdown(),
                nested != null ? nested.stepByStepBreakdown() : null,
                firstNonBlank(request.problemStatement(), nested != null ? nested.problemStatement() : null),
                firstNonBlank(request.finalAnswer(), nested != null ? nested.finalAnswer() : null),
                taxonomy.subtopic(),
                structuredTrap,
                structuredFormulas,
                takeaways,
                quickChecks
        );

        MicroLessonContentDto contentToValidate = new MicroLessonContentDto(
                coreConcept,
                trapText,
                cheatSheetStrings,
                breakdown
        );
        List<String> validationErrors = kaTexValidator.validateContent(contentToValidate, formulaRequired);
        if (!validationErrors.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "KaTeX validation failed: " + String.join(", ", validationErrors)
            );
        }

        String targetStatus = (request.status() != null && !request.status().isBlank())
                ? resolveStatus(request.status())
                : MicroLessonStatus.APPROVED.name();

        int nextVersion = repository.findTopBySubtopicIdOrderByContentVersionDesc(taxonomy.subtopicId())
                .map(prev -> prev.getContentVersion() + 1)
                .orElse(1);

        String cheatSheetJson;
        String stepByStepJson;
        try {
            cheatSheetJson = objectMapper.writeValueAsString(cheatSheetStrings);
            stepByStepJson = objectMapper.writeValueAsString(breakdown);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to serialize micro-lesson content");
        }

        String difficultyLevel = (request.difficultyLevel() != null && !request.difficultyLevel().isBlank())
                ? request.difficultyLevel().trim() : "CORE";
        int seqNo = request.sequenceNo() != null ? request.sequenceNo() : 1;
        int estMins = request.estimatedMinutes() != null ? request.estimatedMinutes() : 5;
        String lang = (request.language() != null && !request.language().isBlank())
                ? request.language().trim() : "en";

        SubtopicMicroLesson entity = SubtopicMicroLesson.builder()
                .subjectId(taxonomy.subjectId())
                .topicId(taxonomy.topicId())
                .subtopicId(taxonomy.subtopicId())
                .coreConcept(coreConcept)
                .trap(trapText)
                .cheatSheetJson(cheatSheetJson)
                .stepByStepBreakdownJson(stepByStepJson)
                .status(targetStatus)
                .reviewedBy(authorId)
                .reviewedAt(Instant.now())
                .reviewComments(request.reviewComments())
                .contentVersion(nextVersion)
                .lessonType(lessonType)
                .difficultyLevel(difficultyLevel)
                .sequenceNo(seqNo)
                .prerequisiteLessonId(request.prerequisiteLessonId())
                .estimatedMinutes(estMins)
                .language(lang)
                .qualityScore(request.qualityScore())
                .build();

        SubtopicMicroLesson saved = repository.save(entity);
        MicroLessonResponse response = toResponse(saved);

        if (MicroLessonStatus.PUBLISHED.name().equals(targetStatus)) {
            cacheService.put(saved.getSubtopicId(), response);
        } else {
            cacheService.evict(saved.getSubtopicId());
        }

        return response;
    }

    @Transactional
    public MicroLessonResponse updateMicroLesson(UUID id, UpdateMicroLessonRequest request, UUID editorId) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request payload cannot be null");
        }

        SubtopicMicroLesson lesson = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Micro-lesson not found"));

        updateTaxonomyIfProvided(lesson, request);

        if (request.lessonType() != null && !request.lessonType().isBlank()) {
            lesson.setLessonType(request.lessonType().trim());
        }
        if (request.difficultyLevel() != null && !request.difficultyLevel().isBlank()) {
            lesson.setDifficultyLevel(request.difficultyLevel().trim());
        }
        if (request.sequenceNo() != null) {
            lesson.setSequenceNo(request.sequenceNo());
        }
        if (request.prerequisiteLessonId() != null) {
            lesson.setPrerequisiteLessonId(request.prerequisiteLessonId());
        }
        if (request.estimatedMinutes() != null) {
            lesson.setEstimatedMinutes(request.estimatedMinutes());
        }
        if (request.language() != null && !request.language().isBlank()) {
            lesson.setLanguage(request.language().trim());
        }
        if (request.qualityScore() != null) {
            lesson.setQualityScore(request.qualityScore());
        }

        MicroLessonStructuredContent nested = request.content();

        String updatedCore = firstNonBlank(request.coreConcept(), nested != null ? nested.coreConcept() : null);
        if (updatedCore != null) {
            lesson.setCoreConcept(updatedCore);
        }

        List<String> existingCheatSheet = parseCheatSheetJson(lesson.getCheatSheetJson());
        StepByStepBreakdownDto existingBreakdown = parseBreakdownJson(lesson.getStepByStepBreakdownJson());

        TrapAlertDto structuredTrap = request.trapAlert() != null
                ? request.trapAlert()
                : (nested != null && nested.trapAlert() != null
                        ? nested.trapAlert()
                        : (existingBreakdown != null ? existingBreakdown.structuredTrapAlert() : null));
        String updatedTrap = resolveTrapText(request.trap(), request.trapAlert() != null ? request.trapAlert() : (nested != null ? nested.trapAlert() : null));
        if (updatedTrap != null && !updatedTrap.isBlank()) {
            lesson.setTrap(updatedTrap);
        }

        boolean explicitCheatSheetUpdate = request.formulaCheatSheet() != null
                || request.cheatSheet() != null
                || (nested != null && nested.formulaCheatSheet() != null);

        List<FormulaEntryDto> structuredFormulas;
        List<String> cheatSheetStrings;
        if (explicitCheatSheetUpdate) {
            List<FormulaEntryDto> candidateFormulas = request.formulaCheatSheet() != null && !request.formulaCheatSheet().isEmpty()
                    ? request.formulaCheatSheet()
                    : (nested != null && nested.formulaCheatSheet() != null && !nested.formulaCheatSheet().isEmpty()
                            ? nested.formulaCheatSheet()
                            : null);
            cheatSheetStrings = resolveCheatSheetStrings(request.cheatSheet(), candidateFormulas);
            structuredFormulas = candidateFormulas != null ? candidateFormulas : Collections.emptyList();
        } else {
            structuredFormulas = existingBreakdown != null ? existingBreakdown.structuredFormulas() : null;
            cheatSheetStrings = existingCheatSheet;
        }

        List<String> takeaways = request.keyTakeaways() != null
                ? request.keyTakeaways()
                : (nested != null && nested.keyTakeaways() != null
                        ? nested.keyTakeaways()
                        : (existingBreakdown != null ? existingBreakdown.keyTakeaways() : null));

        List<QuickCheckItemDto> quickChecks = request.quickCheckItems() != null
                ? request.quickCheckItems()
                : (nested != null && nested.quickCheckItems() != null
                        ? nested.quickCheckItems()
                        : (existingBreakdown != null ? existingBreakdown.quickCheckItems() : null));

        StepByStepBreakdownDto baseBreakdown = request.stepByStepBreakdown() != null
                ? request.stepByStepBreakdown()
                : existingBreakdown;
        List<StepItemDto> nestedSteps = (nested != null && nested.stepByStepBreakdown() != null && !nested.stepByStepBreakdown().isEmpty())
                ? nested.stepByStepBreakdown()
                : (baseBreakdown != null ? baseBreakdown.structuredSteps() : null);

        String resolvedSubtopicName = firstNonBlank(
                request.subtopic(),
                (subtopicRepository != null && lesson.getSubtopicId() != null)
                        ? subtopicRepository.findById(lesson.getSubtopicId()).map(Subtopic::getName).orElse(null)
                        : null,
                "General"
        );
        String resolvedTopicName = firstNonBlank(
                request.topic(),
                (topicRepository != null && lesson.getTopicId() != null)
                        ? topicRepository.findById(lesson.getTopicId()).map(Topic::getName).orElse(null)
                        : null
        );
        String resolvedSubjectName = firstNonBlank(
                request.subject(),
                (subjectRepository != null && lesson.getSubjectId() != null)
                        ? subjectRepository.findById(lesson.getSubjectId()).map(Subject::getName).orElse(null)
                        : null
        );

        StepByStepBreakdownDto mergedBreakdown = buildNormalizedBreakdown(
                baseBreakdown,
                nestedSteps,
                firstNonBlank(request.problemStatement(), nested != null ? nested.problemStatement() : (baseBreakdown != null ? baseBreakdown.questionStem() : null)),
                firstNonBlank(request.finalAnswer(), nested != null ? nested.finalAnswer() : (baseBreakdown != null ? baseBreakdown.finalAnswer() : null)),
                resolvedSubtopicName,
                structuredTrap,
                structuredFormulas,
                takeaways,
                quickChecks
        );

        boolean formulaRequired = blueprintAgent.isFormulaSheetRequired(
                resolvedSubjectName, resolvedTopicName, resolvedSubtopicName, lesson.getLessonType());

        MicroLessonContentDto contentToValidate = new MicroLessonContentDto(
                lesson.getCoreConcept(),
                lesson.getTrap(),
                cheatSheetStrings,
                mergedBreakdown
        );
        List<String> validationErrors = kaTexValidator.validateContent(contentToValidate, formulaRequired);
        if (!validationErrors.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "KaTeX validation failed: " + String.join(", ", validationErrors)
            );
        }

        try {
            lesson.setCheatSheetJson(objectMapper.writeValueAsString(cheatSheetStrings));
            lesson.setStepByStepBreakdownJson(objectMapper.writeValueAsString(mergedBreakdown));
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to serialize updated micro-lesson content");
        }

        if (request.status() != null && !request.status().isBlank()) {
            lesson.setStatus(resolveStatus(request.status()));
        }

        int nextVersion = Math.max(
                lesson.getContentVersion() + 1,
                repository.findTopBySubtopicIdOrderByContentVersionDesc(lesson.getSubtopicId())
                        .map(prev -> prev.getContentVersion() + 1)
                        .orElse(lesson.getContentVersion() + 1)
        );
        lesson.setContentVersion(nextVersion);

        if (editorId != null) {
            lesson.setReviewedBy(editorId);
        }
        lesson.setReviewedAt(Instant.now());
        if (request.reviewComments() != null) {
            lesson.setReviewComments(request.reviewComments());
        }

        SubtopicMicroLesson saved = repository.save(lesson);
        MicroLessonResponse response = toResponse(saved);

        if (MicroLessonStatus.PUBLISHED.name().equals(saved.getStatus())) {
            cacheService.put(saved.getSubtopicId(), response);
        } else {
            cacheService.evict(saved.getSubtopicId());
        }

        return response;
    }

    @Transactional
    public void deleteMicroLesson(UUID id) {
        SubtopicMicroLesson lesson = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Micro-lesson not found"));
        cacheService.evict(lesson.getSubtopicId());
        repository.delete(lesson);
    }

    @Transactional
    public BulkDeleteResponse bulkDelete(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return new BulkDeleteResponse(0);
        }
        List<SubtopicMicroLesson> lessons = repository.findAllById(ids);
        for (SubtopicMicroLesson lesson : lessons) {
            cacheService.evict(lesson.getSubtopicId());
        }
        repository.deleteAll(lessons);
        return new BulkDeleteResponse(lessons.size());
    }

    @Transactional
    public MicroLessonResponse reviewMicroLesson(UUID id, UUID reviewerId, ReviewMicroLessonRequest request) {
        SubtopicMicroLesson lesson = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Micro-lesson not found"));

        if (request == null || request.status() == null || request.status().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required for review transition");
        }

        String rawStatus = request.status().trim().toUpperCase();
        String targetStatus = "EDIT".equals(rawStatus)
                ? (lesson.getStatus() != null ? lesson.getStatus() : MicroLessonStatus.IN_REVIEW.name())
                : resolveStatus(rawStatus);

        // Apply content edits if provided
        if (request.coreConcept() != null && !request.coreConcept().isBlank()) {
            lesson.setCoreConcept(request.coreConcept());
        }
        if (request.trap() != null && !request.trap().isBlank()) {
            lesson.setTrap(request.trap());
        }
        if (request.cheatSheet() != null) {
            try {
                lesson.setCheatSheetJson(objectMapper.writeValueAsString(request.cheatSheet()));
            } catch (JsonProcessingException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cheatSheet format");
            }
        }
        if (request.stepByStepBreakdown() != null) {
            try {
                lesson.setStepByStepBreakdownJson(objectMapper.writeValueAsString(request.stepByStepBreakdown()));
            } catch (JsonProcessingException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid stepByStepBreakdown format");
            }
        }

        // Validate KaTeX expressions on modified content directly from the updated entity
        List<String> cheatSheet = parseCheatSheetJson(lesson.getCheatSheetJson());
        StepByStepBreakdownDto breakdown = parseBreakdownJson(lesson.getStepByStepBreakdownJson());

        String reviewSubject = (subjectRepository != null && lesson.getSubjectId() != null)
                ? subjectRepository.findById(lesson.getSubjectId()).map(Subject::getName).orElse(null)
                : null;
        String reviewTopic = (topicRepository != null && lesson.getTopicId() != null)
                ? topicRepository.findById(lesson.getTopicId()).map(Topic::getName).orElse(null)
                : null;
        String reviewSubtopic = (subtopicRepository != null && lesson.getSubtopicId() != null)
                ? subtopicRepository.findById(lesson.getSubtopicId()).map(Subtopic::getName).orElse(null)
                : null;
        boolean formulaRequired = (reviewSubject != null || reviewTopic != null || reviewSubtopic != null)
                && blueprintAgent.isFormulaSheetRequired(reviewSubject, reviewTopic, reviewSubtopic, lesson.getLessonType());

        MicroLessonContentDto contentToValidate = new MicroLessonContentDto(
                lesson.getCoreConcept(),
                lesson.getTrap(),
                cheatSheet,
                breakdown
        );
        List<String> validationErrors = kaTexValidator.validateContent(contentToValidate, formulaRequired);
        if (!validationErrors.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "KaTeX validation failed: " + String.join(", ", validationErrors));
        }

        lesson.setStatus(targetStatus);
        lesson.setReviewedBy(reviewerId);
        lesson.setReviewedAt(Instant.now());
        if (request.comments() != null) {
            lesson.setReviewComments(request.comments());
        }

        SubtopicMicroLesson saved = repository.save(lesson);
        MicroLessonResponse response = toResponse(saved);

        // Update / evict cache
        if (MicroLessonStatus.PUBLISHED.name().equals(targetStatus)) {
            cacheService.put(saved.getSubtopicId(), response);
        } else {
            cacheService.evict(saved.getSubtopicId());
        }

        return response;
    }

    @Transactional
    public MicroLessonResponse generateSingleMicroLesson(GenerateMicroLessonRequest request) {
        if (request == null || request.subtopic() == null || request.subtopic().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subtopic is required for generation");
        }
        Long subjectId = request.subjectId() != null ? request.subjectId() : 1L;
        String subject = (request.subject() != null && !request.subject().isBlank()) ? request.subject() : "General";
        Long topicId = request.topicId() != null ? request.topicId() : 1L;
        String topic = (request.topic() != null && !request.topic().isBlank()) ? request.topic() : "General";
        Long subtopicId = request.subtopicId() != null ? request.subtopicId() : (long) Math.abs(request.subtopic().hashCode());
        String targetExam = (request.targetExam() != null && !request.targetExam().isBlank()) ? request.targetExam() : "JEE_MAIN";

        String reference = request.referenceText();
        if (reference == null || reference.isBlank()) {
            reference = request.promptNotes();
        }

        SubtopicMicroLesson lesson = generatorService.generateForSubtopic(
                subjectId, subject, topicId, topic, subtopicId, request.subtopic(), targetExam, reference
        );
        return toResponse(lesson);
    }

    private record ResolvedTaxonomy(
            Long subjectId,
            String subject,
            Long topicId,
            String topic,
            Long subtopicId,
            String subtopic
    ) {}

    private ResolvedTaxonomy resolveTaxonomyForCreate(
            Long subjectId, String subjectName,
            Long topicId, String topicName,
            Long subtopicId, String subtopicName) {

        Long resolvedSubjectId = subjectId;
        String resolvedSubject = (subjectName != null && !subjectName.isBlank()) ? subjectName.trim() : null;

        if (resolvedSubject == null && subjectRepository != null && resolvedSubjectId != null) {
            Subject sub = subjectRepository.findById(resolvedSubjectId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject not found: " + subjectId));
            resolvedSubject = sub.getName();
        }
        if (resolvedSubject == null || resolvedSubject.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject is required");
        }
        if (resolvedSubjectId == null) {
            resolvedSubjectId = 1L;
        }

        Long resolvedTopicId = topicId;
        String resolvedTopic = (topicName != null && !topicName.isBlank()) ? topicName.trim() : null;

        if (resolvedTopic == null && topicRepository != null && resolvedTopicId != null) {
            Topic top = topicRepository.findById(resolvedTopicId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic not found: " + topicId));
            if (top.getSubjectId() != null && subjectId != null && !top.getSubjectId().equals(resolvedSubjectId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic does not belong to specified subject");
            }
            resolvedTopic = top.getName();
        }
        if (resolvedTopic == null || resolvedTopic.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic is required");
        }
        if (resolvedTopicId == null) {
            resolvedTopicId = 1L;
        }

        Long resolvedSubtopicId = subtopicId;
        String resolvedSubtopic = (subtopicName != null && !subtopicName.isBlank()) ? subtopicName.trim() : null;

        if (resolvedSubtopic == null && subtopicRepository != null && resolvedSubtopicId != null) {
            Subtopic st = subtopicRepository.findById(resolvedSubtopicId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subtopic not found: " + subtopicId));
            if (st.getTopicId() != null && topicId != null && !st.getTopicId().equals(resolvedTopicId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subtopic does not belong to specified topic");
            }
            resolvedSubtopic = st.getName();
        }
        if (resolvedSubtopic == null || resolvedSubtopic.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subtopic is required");
        }
        if (resolvedSubtopicId == null) {
            long hash = Math.abs((long) resolvedSubtopic.toLowerCase().hashCode());
            resolvedSubtopicId = hash > 0 ? hash : 1L;
        }

        return new ResolvedTaxonomy(
                resolvedSubjectId,
                resolvedSubject,
                resolvedTopicId,
                resolvedTopic,
                resolvedSubtopicId,
                resolvedSubtopic
        );
    }

    private void updateTaxonomyIfProvided(SubtopicMicroLesson lesson, UpdateMicroLessonRequest request) {
        if (request.subjectId() != null) {
            lesson.setSubjectId(request.subjectId());
        }
        if (request.topicId() != null) {
            lesson.setTopicId(request.topicId());
        }
        if (request.subtopicId() != null) {
            lesson.setSubtopicId(request.subtopicId());
        }
    }

    private String resolveStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return MicroLessonStatus.APPROVED.name();
        }
        String normalized = raw.trim().toUpperCase();
        return switch (normalized) {
            case "REJECT", "REJECTED" -> MicroLessonStatus.REJECTED.name();
            case "APPROVE", "APPROVED" -> MicroLessonStatus.APPROVED.name();
            case "PUBLISH", "PUBLISHED" -> MicroLessonStatus.PUBLISHED.name();
            case "IN_REVIEW", "PENDING_REVIEW" -> MicroLessonStatus.IN_REVIEW.name();
            case "AI_GENERATED" -> MicroLessonStatus.AI_GENERATED.name();
            case "DRAFT" -> MicroLessonStatus.DRAFT.name();
            default -> {
                try {
                    yield MicroLessonStatus.valueOf(normalized).name();
                } catch (IllegalArgumentException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + raw);
                }
            }
        };
    }

    private String resolveTrapText(String rawTrap, TrapAlertDto trapAlert) {
        if (rawTrap != null && !rawTrap.isBlank()) {
            return rawTrap.trim();
        }
        if (trapAlert != null) {
            if (trapAlert.description() != null && !trapAlert.description().isBlank()) {
                return trapAlert.description().trim();
            }
            if (trapAlert.commonMistake() != null && !trapAlert.commonMistake().isBlank()) {
                return trapAlert.commonMistake().trim();
            }
            if (trapAlert.title() != null && !trapAlert.title().isBlank()) {
                return trapAlert.title().trim();
            }
        }
        return null;
    }

    private List<String> resolveCheatSheetStrings(List<String> rawCheatSheet, List<FormulaEntryDto> formulaEntries) {
        List<String> result = new ArrayList<>();
        if (formulaEntries != null && !formulaEntries.isEmpty()) {
            for (FormulaEntryDto f : formulaEntries) {
                if (f == null) continue;
                String name = f.name() != null ? f.name().trim() : "";
                String latex = f.latex() != null ? f.latex().trim() : "";
                if (!name.isEmpty() && !latex.isEmpty()) {
                    result.add(name + ": " + latex);
                } else if (!latex.isEmpty()) {
                    result.add(latex);
                } else if (!name.isEmpty()) {
                    result.add(name);
                }
            }
        } else if (rawCheatSheet != null) {
            for (String item : rawCheatSheet) {
                if (item != null && !item.isBlank()) {
                    result.add(item.trim());
                }
            }
        }
        return result;
    }

    private StepByStepBreakdownDto buildNormalizedBreakdown(
            StepByStepBreakdownDto breakdown,
            List<StepItemDto> fallbackStructuredSteps,
            String fallbackQuestionStem,
            String fallbackFinalAnswer,
            String subtopicName,
            TrapAlertDto structuredTrap,
            List<FormulaEntryDto> structuredFormulas,
            List<String> keyTakeaways,
            List<QuickCheckItemDto> quickCheckItems) {

        List<StepItemDto> structuredSteps = (breakdown != null && breakdown.structuredSteps() != null && !breakdown.structuredSteps().isEmpty())
                ? breakdown.structuredSteps()
                : fallbackStructuredSteps;

        List<String> rawSteps = new ArrayList<>();
        if (breakdown != null && breakdown.steps() != null && !breakdown.steps().isEmpty()) {
            for (String s : breakdown.steps()) {
                if (s != null && !s.isBlank()) {
                    rawSteps.add(s.trim());
                }
            }
        } else if (structuredSteps != null && !structuredSteps.isEmpty()) {
            for (StepItemDto item : structuredSteps) {
                if (item == null) continue;
                StringBuilder sb = new StringBuilder();
                if (item.explanation() != null && !item.explanation().isBlank()) {
                    sb.append(item.explanation().trim());
                } else if (item.title() != null && !item.title().isBlank()) {
                    sb.append(item.title().trim());
                }
                if (item.formula() != null && !item.formula().isBlank() && !sb.toString().contains(item.formula())) {
                    if (!sb.isEmpty()) sb.append(" ");
                    sb.append(item.formula().trim());
                }
                if (!sb.isEmpty()) {
                    rawSteps.add(sb.toString());
                }
            }
        }

        String stem = firstNonBlank(
                breakdown != null ? breakdown.questionStem() : null,
                fallbackQuestionStem
        );
        if ((stem == null || stem.isBlank()) && !rawSteps.isEmpty()) {
            stem = "Worked Example: " + (subtopicName != null ? subtopicName : "Concept Application");
        }

        String finalAns = firstNonBlank(
                breakdown != null ? breakdown.finalAnswer() : null,
                fallbackFinalAnswer
        );
        if ((finalAns == null || finalAns.isBlank()) && !rawSteps.isEmpty()) {
            if (structuredSteps != null && !structuredSteps.isEmpty()) {
                StepItemDto last = structuredSteps.get(structuredSteps.size() - 1);
                finalAns = firstNonBlank(last.tip(), last.formula(), last.explanation(), "Verified solution");
            } else {
                finalAns = rawSteps.get(rawSteps.size() - 1);
            }
        }

        return new StepByStepBreakdownDto(
                stem,
                rawSteps,
                finalAns,
                structuredSteps,
                structuredTrap,
                structuredFormulas,
                keyTakeaways,
                quickCheckItems
        );
    }

    private List<String> parseCheatSheetJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    private StepByStepBreakdownDto parseBreakdownJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, StepByStepBreakdownDto.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private Page<MicroLessonResponse> toResponsePage(Page<SubtopicMicroLesson> page) {
        List<SubtopicMicroLesson> content = page.getContent();
        if (content.isEmpty()) {
            return page.map(this::toResponse);
        }

        Set<Long> subjectIds = content.stream()
                .map(SubtopicMicroLesson::getSubjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> topicIds = content.stream()
                .map(SubtopicMicroLesson::getTopicId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> subtopicIds = content.stream()
                .map(SubtopicMicroLesson::getSubtopicId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> subjectNames = new HashMap<>();
        if (subjectRepository != null && !subjectIds.isEmpty()) {
            try {
                subjectRepository.findAllById(subjectIds).forEach(s -> subjectNames.put(s.getId(), s.getName()));
            } catch (Exception ignored) {}
        }

        Map<Long, String> topicNames = new HashMap<>();
        if (topicRepository != null && !topicIds.isEmpty()) {
            try {
                topicRepository.findAllById(topicIds).forEach(t -> topicNames.put(t.getId(), t.getName()));
            } catch (Exception ignored) {}
        }

        Map<Long, String> subtopicNames = new HashMap<>();
        if (subtopicRepository != null && !subtopicIds.isEmpty()) {
            try {
                subtopicRepository.findAllById(subtopicIds).forEach(st -> subtopicNames.put(st.getId(), st.getName()));
            } catch (Exception ignored) {}
        }

        return page.map(entity -> {
            MicroLessonResponse base = (generatorService != null) ? generatorService.toResponse(entity) : null;
            return enrichWithTaxonomy(
                    base,
                    entity,
                    subjectNames.get(entity.getSubjectId()),
                    topicNames.get(entity.getTopicId()),
                    subtopicNames.get(entity.getSubtopicId())
            );
        });
    }

    private MicroLessonResponse enrichWithTaxonomy(
            MicroLessonResponse base,
            SubtopicMicroLesson entity,
            String sName,
            String tName,
            String stName) {

        if (base == null) {
            return new MicroLessonResponse(
                    entity.getId(),
                    entity.getSubjectId(),
                    entity.getTopicId(),
                    entity.getSubtopicId(),
                    sName,
                    tName,
                    stName,
                    null,
                    entity.getCoreConcept(),
                    entity.getTrap(),
                    parseCheatSheetJson(entity.getCheatSheetJson()),
                    parseBreakdownJson(entity.getStepByStepBreakdownJson()),
                    entity.getStatus(),
                    entity.getReviewedBy(),
                    entity.getReviewedAt(),
                    entity.getReviewComments(),
                    entity.getContentVersion(),
                    entity.getCreatedAt(),
                    entity.getUpdatedAt(),
                    entity.getLanguage(),
                    false,
                    entity.getLessonType(),
                    entity.getDifficultyLevel(),
                    entity.getSequenceNo(),
                    entity.getPrerequisiteLessonId(),
                    entity.getEstimatedMinutes(),
                    entity.getQualityScore()
            );
        }

        String resolvedSubject = base.subject() != null ? base.subject() : sName;
        String resolvedTopic = base.topic() != null ? base.topic() : tName;
        String resolvedSubtopic = base.subtopic() != null ? base.subtopic() : stName;

        return new MicroLessonResponse(
                base.id(),
                base.subjectId(),
                base.topicId(),
                base.subtopicId(),
                resolvedSubject,
                resolvedTopic,
                resolvedSubtopic,
                base.targetExam(),
                base.coreConcept(),
                base.trap(),
                base.cheatSheet(),
                base.stepByStepBreakdown(),
                base.status(),
                base.reviewedBy(),
                base.reviewedAt(),
                base.reviewComments(),
                base.contentVersion(),
                base.createdAt(),
                base.updatedAt(),
                base.languageCode(),
                base.fallbackToEnglish(),
                base.lessonType(),
                base.difficultyLevel(),
                base.sequenceNo(),
                base.prerequisiteLessonId(),
                base.estimatedMinutes(),
                base.qualityScore()
        );
    }

    private MicroLessonResponse toResponse(SubtopicMicroLesson entity) {
        if (entity == null) return null;

        String sName = null, tName = null, stName = null;
        if (subtopicRepository != null && entity.getSubtopicId() != null) {
            try {
                stName = subtopicRepository.findById(entity.getSubtopicId()).map(Subtopic::getName).orElse(null);
            } catch (Exception ignored) {}
        }
        if (topicRepository != null && entity.getTopicId() != null) {
            try {
                tName = topicRepository.findById(entity.getTopicId()).map(Topic::getName).orElse(null);
            } catch (Exception ignored) {}
        }
        if (subjectRepository != null && entity.getSubjectId() != null) {
            try {
                sName = subjectRepository.findById(entity.getSubjectId()).map(Subject::getName).orElse(null);
            } catch (Exception ignored) {}
        }

        MicroLessonResponse base = (generatorService != null) ? generatorService.toResponse(entity) : null;
        return enrichWithTaxonomy(base, entity, sName, tName, stName);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    @Transactional
    public Optional<MicroLessonResponse> getPublishedMicroLesson(
            Long subtopicId,
            String targetExam,
            String subjectName,
            String topicName,
            String subtopicName,
            String lang) {

        Optional<MicroLessonResponse> baseOpt = getPublishedMicroLesson(
                subtopicId, targetExam, subjectName, topicName, subtopicName);

        if (baseOpt.isEmpty()) {
            return Optional.empty();
        }

        MicroLessonResponse base = baseOpt.get();
        if (lang == null || lang.isBlank() || "en".equalsIgnoreCase(lang.trim()) || "eng".equalsIgnoreCase(lang.trim())) {
            return Optional.of(base.withLocalization("en", false));
        }

        String targetLang = lang.trim().toLowerCase();
        if (translationWorkflowService != null && base.id() != null) {
            String tenantId = TenantContext.get();
            try {
                MicroLessonResponse localized = translationWorkflowService.getLocalizedMicroLesson(base.id(), targetLang, tenantId);
                return Optional.of(localized);
            } catch (Exception e) {
                log.warn("Failed to retrieve localized micro-lesson for id={} lang={}: {}", base.id(), targetLang, e.getMessage());
            }
        }

        return Optional.of(base.withLocalization("en", true));
    }

    @Transactional
    public Optional<MicroLessonResponse> getPublishedMicroLesson(Long subtopicId, String targetExam) {
        return getPublishedMicroLesson(subtopicId, targetExam, null, null, null);
    }

    @Transactional
    public Optional<MicroLessonResponse> getPublishedMicroLesson(
            Long subtopicId,
            String targetExam,
            String subjectName,
            String topicName,
            String subtopicName) {

        // 1. Check Redis cache first
        if (subtopicId != null && subtopicId > 0) {
            Optional<MicroLessonResponse> cached = cacheService.getCached(subtopicId);
            if (cached.isPresent()) {
                return cached;
            }
        }

        String exam = (targetExam != null && !targetExam.isBlank()) ? targetExam : "JEE_MAIN";

        // 2. Fetch PUBLISHED from database if available
        if (subtopicId != null && subtopicId > 0) {
            Optional<SubtopicMicroLesson> publishedLesson = repository
                    .findFirstBySubtopicIdAndStatusOrderByContentVersionDesc(
                            subtopicId, MicroLessonStatus.PUBLISHED.name());
            if (publishedLesson.isPresent()) {
                MicroLessonResponse response = toResponse(publishedLesson.get());
                cacheService.put(subtopicId, response);
                return Optional.of(response);
            }

            // 3. Next check if any micro-lesson (AI_GENERATED, PENDING_REVIEW, PUBLISHED) exists
            Optional<SubtopicMicroLesson> anyLesson = repository
                    .findTopBySubtopicIdOrderByContentVersionDesc(subtopicId);
            if (anyLesson.isPresent()) {
                MicroLessonResponse response = toResponse(anyLesson.get());
                return Optional.of(response);
            }
        }

        // 4. Resolve taxonomy names (from repositories or query parameter fallbacks)
        Long resolvedSubjectId = 1L;
        String resolvedSubject = (subjectName != null && !subjectName.isBlank()) ? subjectName.trim() : null;
        Long resolvedTopicId = 1L;
        String resolvedTopic = (topicName != null && !topicName.isBlank()) ? topicName.trim() : null;
        String resolvedSubtopic = (subtopicName != null && !subtopicName.isBlank()) ? subtopicName.trim() : null;
        Long effectiveSubtopicId = (subtopicId != null && subtopicId > 0) ? subtopicId : null;

        if (subtopicRepository != null && effectiveSubtopicId != null) {
            try {
                Optional<Subtopic> stOpt = subtopicRepository.findById(effectiveSubtopicId);
                if (stOpt.isPresent()) {
                    Subtopic st = stOpt.get();
                    if (resolvedSubtopic == null) {
                        resolvedSubtopic = st.getName();
                    }
                    if (st.getTopicId() != null) {
                        resolvedTopicId = st.getTopicId();
                        if (topicRepository != null) {
                            Optional<Topic> topOpt = topicRepository.findById(resolvedTopicId);
                            if (topOpt.isPresent()) {
                                Topic top = topOpt.get();
                                if (resolvedTopic == null) {
                                    resolvedTopic = top.getName();
                                }
                                if (top.getSubjectId() != null) {
                                    resolvedSubjectId = top.getSubjectId();
                                    if (subjectRepository != null) {
                                        Optional<Subject> subOpt = subjectRepository.findById(resolvedSubjectId);
                                        if (subOpt.isPresent() && resolvedSubject == null) {
                                            resolvedSubject = subOpt.get().getName();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                log.debug("Could not resolve subtopic from DB repository for ID {}: {}", effectiveSubtopicId, ex.getMessage());
            }
        }

        // If subtopic name cannot be determined, return empty (404 Not Found)
        if (resolvedSubtopic == null || resolvedSubtopic.isBlank()) {
            return Optional.empty();
        }

        if (effectiveSubtopicId == null) {
            long hash = Math.abs((long) resolvedSubtopic.toLowerCase().trim().hashCode());
            effectiveSubtopicId = hash > 0 ? hash : 1L;
        }

        if (resolvedSubject == null || resolvedSubject.isBlank()) {
            resolvedSubject = "General";
        }
        if (resolvedTopic == null || resolvedTopic.isBlank()) {
            resolvedTopic = "General";
        }

        // 5. Synthesize on demand
        try {
            SubtopicMicroLesson generated = generatorService.generateForSubtopic(
                    resolvedSubjectId,
                    resolvedSubject,
                    resolvedTopicId,
                    resolvedTopic,
                    effectiveSubtopicId,
                    resolvedSubtopic,
                    exam
            );
            MicroLessonResponse response = toResponse(generated);
            cacheService.put(effectiveSubtopicId, response);
            return Optional.of(response);
        } catch (Exception ex) {
            log.warn("On-demand synthesis failed for subtopic {}: {}. Generating fallback content.",
                    effectiveSubtopicId, ex.getMessage());
            try {
                MicroLessonContentDto fallbackContent = generatorService.buildFallbackContent(
                        resolvedSubject, resolvedTopic, resolvedSubtopic, exam);
                SubtopicMicroLesson transientLesson = SubtopicMicroLesson.builder()
                        .subjectId(resolvedSubjectId)
                        .topicId(resolvedTopicId)
                        .subtopicId(effectiveSubtopicId)
                        .coreConcept(fallbackContent.coreConcept())
                        .trap(fallbackContent.trap())
                        .cheatSheetJson(objectMapper.writeValueAsString(fallbackContent.cheatSheet()))
                        .stepByStepBreakdownJson(objectMapper.writeValueAsString(fallbackContent.stepByStepBreakdown()))
                        .status(MicroLessonStatus.AI_GENERATED.name())
                        .contentVersion(1)
                        .lessonType("CONCEPT")
                        .difficultyLevel("CORE")
                        .sequenceNo(1)
                        .estimatedMinutes(5)
                        .language("en")
                        .build();
                return Optional.of(toResponse(transientLesson));
            } catch (Exception e2) {
                log.error("Failed to construct fallback micro-lesson: {}", e2.getMessage());
                return Optional.empty();
            }
        }
    }

    /**
     * Retrieves all micro-lessons for a subtopic with optional filters (v2).
     */
    @Transactional(readOnly = true)
    public List<MicroLessonResponse> getMicroLessonsForSubtopic(Long subtopicId, String lessonType, String difficulty, String language) {
        if (subtopicId == null || subtopicId <= 0) {
            return Collections.emptyList();
        }

        List<SubtopicMicroLesson> lessons = repository.findBySubtopicIdAndStatusOrderBySequenceNoAsc(
                subtopicId, MicroLessonStatus.PUBLISHED.name());
        if (lessons.isEmpty()) {
            lessons = repository.findBySubtopicIdOrderBySequenceNoAsc(subtopicId);
        }

        List<MicroLessonResponse> responses = new ArrayList<>();
        for (SubtopicMicroLesson lesson : lessons) {
            if (lessonType != null && !lessonType.isBlank() && !lessonType.equalsIgnoreCase(lesson.getLessonType())) {
                continue;
            }
            if (difficulty != null && !difficulty.isBlank() && !difficulty.equalsIgnoreCase(lesson.getDifficultyLevel())) {
                continue;
            }
            MicroLessonResponse res = toResponse(lesson);
            if (language != null && !language.isBlank() && !"en".equalsIgnoreCase(language.trim())) {
                if (translationWorkflowService != null && res.id() != null) {
                    try {
                        res = translationWorkflowService.getLocalizedMicroLesson(res.id(), language.trim().toLowerCase(), TenantContext.get());
                    } catch (Exception e) {
                        res = res.withLocalization("en", true);
                    }
                }
            }
            responses.add(res);
        }
        return responses;
    }

    /**
     * Learner lesson view telemetry hook.
     */
    @Transactional
    public void recordLessonView(UUID lessonId) {
        if (lessonId == null || effectivenessRepository == null) return;
        com.examplatform.questionbank.microlesson.domain.LessonEffectiveness eff = effectivenessRepository.findById(lessonId)
                .orElseGet(() -> com.examplatform.questionbank.microlesson.domain.LessonEffectiveness.builder()
                        .lessonId(lessonId)
                        .views(0L)
                        .completions(0L)
                        .postLessonAccuracyDelta(0.0)
                        .build());
        eff.setViews(eff.getViews() + 1);
        effectivenessRepository.save(eff);
        log.debug("Recorded view for lesson: {}", lessonId);
    }

    /**
     * Learner lesson completion telemetry hook.
     */
    @Transactional
    public void recordLessonCompletion(UUID lessonId, Double postAccuracyDelta) {
        if (lessonId == null || effectivenessRepository == null) return;
        com.examplatform.questionbank.microlesson.domain.LessonEffectiveness eff = effectivenessRepository.findById(lessonId)
                .orElseGet(() -> com.examplatform.questionbank.microlesson.domain.LessonEffectiveness.builder()
                        .lessonId(lessonId)
                        .views(0L)
                        .completions(0L)
                        .postLessonAccuracyDelta(0.0)
                        .build());
        eff.setCompletions(eff.getCompletions() + 1);
        if (postAccuracyDelta != null) {
            eff.setPostLessonAccuracyDelta(postAccuracyDelta);
        }
        effectivenessRepository.save(eff);
        log.debug("Recorded completion for lesson: {}, delta: {}", lessonId, postAccuracyDelta);
    }

    /**
     * Learner progress handler.
     */
    @Transactional
    public void recordProgress(LessonProgressRequest request) {
        if (request == null || request.getLessonId() == null) return;
        if ("COMPLETED".equalsIgnoreCase(request.getStatus())) {
            recordLessonCompletion(request.getLessonId(), request.getPostAccuracyDelta());
        } else {
            recordLessonView(request.getLessonId());
        }
    }
}
