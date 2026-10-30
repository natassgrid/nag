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
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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

    private static final String FIELD_SUBJECT_ID = "subjectId";
    private static final String DEFAULT_GENERAL = "General";
    private static final String ERR_MICRO_LESSON_NOT_FOUND = "Micro-lesson not found";

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
    private final MicroLessonService self;

    public MicroLessonService(
            SubtopicMicroLessonRepository repository,
            MicroLessonGeneratorService generatorService,
            MicroLessonCacheService cacheService,
            KaTexValidator kaTexValidator,
            ObjectMapper objectMapper) {
        this(repository, generatorService, cacheService, kaTexValidator, objectMapper, null, null, null, null, null, null);
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
        this(repository, generatorService, cacheService, kaTexValidator, objectMapper, subtopicRepository, topicRepository, subjectRepository, null, null, null);
    }

    public MicroLessonService(
            SubtopicMicroLessonRepository repository,
            MicroLessonGeneratorService generatorService,
            MicroLessonCacheService cacheService,
            KaTexValidator kaTexValidator,
            ObjectMapper objectMapper,
            SubtopicRepository subtopicRepository,
            TopicRepository topicRepository,
            SubjectRepository subjectRepository,
            MicroLessonTranslationWorkflowService translationWorkflowService,
            com.examplatform.questionbank.microlesson.repository.LessonEffectivenessRepository effectivenessRepository) {
        this(repository, generatorService, cacheService, kaTexValidator, objectMapper, subtopicRepository, topicRepository, subjectRepository, translationWorkflowService, effectivenessRepository, null);
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
            @Autowired(required = false) com.examplatform.questionbank.microlesson.repository.LessonEffectivenessRepository effectivenessRepository,
            @Autowired(required = false) @Lazy MicroLessonService self) {
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
        this.self = self != null ? self : this;
    }

    @Transactional(readOnly = true)
    public Page<MicroLessonResponse> listForReview(String status, Long subjectId, Pageable pageable) {
        return self.listForReview(status, subjectId, null, null, null, null, pageable);
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
        if (!hasExtendedReviewFilters(subject, topic, targetExam, search, status)) {
            result = findWithoutExtendedFilters(status, subjectId, pageable);
        } else {
            Specification<SubtopicMicroLesson> spec = buildSpecification(status, subjectId, subject, topic, search);
            result = repository.findAll(spec, pageable);
        }

        return toResponsePage(result);
    }

    private boolean hasExtendedReviewFilters(String subject, String topic, String targetExam, String search, String status) {
        return (subject != null && !subject.isBlank() && !"ALL".equalsIgnoreCase(subject))
                || (topic != null && !topic.isBlank() && !"ALL".equalsIgnoreCase(topic))
                || (targetExam != null && !targetExam.isBlank() && !"ALL".equalsIgnoreCase(targetExam))
                || (search != null && !search.isBlank())
                || (status != null && "ALL".equalsIgnoreCase(status.trim()));
    }

    private Page<SubtopicMicroLesson> findWithoutExtendedFilters(String status, Long subjectId, Pageable pageable) {
        if (status != null && !status.isBlank() && subjectId != null) {
            return repository.findByStatusAndSubjectId(status.toUpperCase(), subjectId, pageable);
        } else if (status != null && !status.isBlank()) {
            return repository.findByStatus(status.toUpperCase(), pageable);
        } else if (subjectId != null) {
            return repository.findBySubjectId(subjectId, pageable);
        }
        return repository.findByStatus(MicroLessonStatus.AI_GENERATED.name(), pageable);
    }

    private Specification<SubtopicMicroLesson> buildSpecification(
            String status,
            Long subjectId,
            String subject,
            String topic,
            String search) {
        return (root, _, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            addStatusPredicate(cb, root, predicates, status);
            Long resolvedSubjectId = addSubjectPredicate(cb, root, predicates, subjectId, subject);
            addTopicPredicate(cb, root, predicates, topic, resolvedSubjectId);
            addSearchPredicate(cb, root, predicates, search);
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void addStatusPredicate(CriteriaBuilder cb, Root<SubtopicMicroLesson> root, List<Predicate> predicates, String status) {
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
            predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
        }
    }

    private Long addSubjectPredicate(CriteriaBuilder cb, Root<SubtopicMicroLesson> root, List<Predicate> predicates, Long subjectId, String subject) {
        if (subjectId != null) {
            predicates.add(cb.equal(root.get(FIELD_SUBJECT_ID), subjectId));
            return subjectId;
        }
        if (subject != null && !subject.isBlank() && !"ALL".equalsIgnoreCase(subject.trim())) {
            List<Long> matchedSubjectIds = resolveSubjectIds(subject.trim());
            if (!matchedSubjectIds.isEmpty()) {
                predicates.add(root.get(FIELD_SUBJECT_ID).in(matchedSubjectIds));
                if (matchedSubjectIds.size() == 1) {
                    return matchedSubjectIds.getFirst();
                }
            } else {
                predicates.add(cb.disjunction());
            }
        }
        return null;
    }

    private void addTopicPredicate(CriteriaBuilder cb, Root<SubtopicMicroLesson> root, List<Predicate> predicates, String topic, Long resolvedSubjectId) {
        if (topic != null && !topic.isBlank() && !"ALL".equalsIgnoreCase(topic.trim())) {
            List<Long> matchedTopicIds = resolveTopicIds(topic.trim(), resolvedSubjectId);
            if (!matchedTopicIds.isEmpty()) {
                predicates.add(root.get("topicId").in(matchedTopicIds));
            } else {
                predicates.add(cb.disjunction());
            }
        }
    }

    private void addSearchPredicate(CriteriaBuilder cb, Root<SubtopicMicroLesson> root, List<Predicate> predicates, String search) {
        if (search == null || search.isBlank()) {
            return;
        }
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
            searchOr.add(root.get(FIELD_SUBJECT_ID).in(subjectIds));
        }

        predicates.add(cb.or(searchOr.toArray(new Predicate[0])));
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
        String trimmed = subjectStr.trim();
        List<Long> exact = findExactSubjectIds(trimmed);
        if (!exact.isEmpty()) {
            return exact;
        }
        List<Long> ignoreCase = findCaseInsensitiveSubjectIds(trimmed);
        if (!ignoreCase.isEmpty()) {
            return ignoreCase;
        }
        return scanAllSubjectIds(trimmed);
    }

    private List<Long> findExactSubjectIds(String subjectStr) {
        try {
            Optional<Subject> exact = subjectRepository.findByNameAndTenantIdIsNull(subjectStr);
            if (exact.isPresent()) {
                return List.of(exact.get().getId());
            }
        } catch (Exception _) {
            // Fallback to case-insensitive lookup
        }
        return Collections.emptyList();
    }

    private List<Long> findCaseInsensitiveSubjectIds(String subjectStr) {
        try {
            List<Subject> byIgnoreCase = subjectRepository.findByNameIgnoreCaseAndTenantIdIsNull(subjectStr);
            if (!byIgnoreCase.isEmpty()) {
                return byIgnoreCase.stream().map(Subject::getId).filter(Objects::nonNull).toList();
            }
        } catch (Exception _) {
            // Fallback to table scan
        }
        return Collections.emptyList();
    }

    private List<Long> scanAllSubjectIds(String subjectStr) {
        try {
            return subjectRepository.findAll().stream()
                    .filter(s -> s.getName() != null && s.getName().equalsIgnoreCase(subjectStr))
                    .map(Subject::getId)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception _) {
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
        String trimmed = topicStr.trim();
        List<Long> exact = findExactTopicIds(trimmed, subjectId);
        if (!exact.isEmpty()) {
            return exact;
        }
        List<Long> ignoreCase = findCaseInsensitiveTopicIds(trimmed, subjectId);
        if (!ignoreCase.isEmpty()) {
            return ignoreCase;
        }
        return scanAllTopicIds(trimmed, subjectId);
    }

    private List<Long> findExactTopicIds(String topicStr, Long subjectId) {
        try {
            if (subjectId != null) {
                Optional<Topic> exact = topicRepository.findByNameAndSubjectIdAndTenantIdIsNull(topicStr, subjectId);
                if (exact.isPresent()) {
                    return List.of(exact.get().getId());
                }
            }
        } catch (Exception _) {
            // Fallback to case-insensitive topic search
        }
        return Collections.emptyList();
    }

    private List<Long> findCaseInsensitiveTopicIds(String topicStr, Long subjectId) {
        try {
            List<Topic> byIgnoreCase = topicRepository.findByNameIgnoreCaseAndTenantIdIsNull(topicStr);
            if (!byIgnoreCase.isEmpty()) {
                return byIgnoreCase.stream()
                        .filter(t -> subjectId == null || subjectId.equals(t.getSubjectId()))
                        .map(Topic::getId)
                        .filter(Objects::nonNull)
                        .toList();
            }
        } catch (Exception _) {
            // Fallback to repository stream
        }
        return Collections.emptyList();
    }

    private List<Long> scanAllTopicIds(String topicStr, Long subjectId) {
        try {
            return topicRepository.findAll().stream()
                    .filter(t -> t.getName() != null && t.getName().equalsIgnoreCase(topicStr))
                    .filter(t -> subjectId == null || subjectId.equals(t.getSubjectId()))
                    .map(Topic::getId)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception _) {
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
        } catch (Exception _) {
            // Fallback to findAll scan
        }
        try {
            return subtopicRepository.findAll().stream()
                    .filter(st -> st.getName() != null && st.getName().toLowerCase().contains(term.toLowerCase()))
                    .map(Subtopic::getId)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception _) {
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
        } catch (Exception _) {
            // Fallback to findAll scan
        }
        try {
            return topicRepository.findAll().stream()
                    .filter(t -> t.getName() != null && t.getName().toLowerCase().contains(term.toLowerCase()))
                    .map(Topic::getId)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception _) {
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
        } catch (Exception _) {
            // Fallback to findAll scan
        }
        try {
            return subjectRepository.findAll().stream()
                    .filter(s -> s.getName() != null && s.getName().toLowerCase().contains(term.toLowerCase()))
                    .map(Subject::getId)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception _) {
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
            } catch (Exception _) {
                topics = List.of();
            }
        }
        List<String> statuses = List.of("ALL", "AI_GENERATED", "IN_REVIEW", "APPROVED", "PUBLISHED", "REJECTED");
        return new MicroLessonFilterOptionsResponse(subjects, topics, statuses);
    }

    @Transactional(readOnly = true)
    public MicroLessonResponse getMicroLessonById(UUID id) {
        SubtopicMicroLesson lesson = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ERR_MICRO_LESSON_NOT_FOUND));
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

        MicroLessonStructuredContent nested = request.content();
        String coreConcept = firstNonBlank(request.coreConcept(), nested != null ? nested.coreConcept() : null);
        if (coreConcept == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Core concept cannot be empty");
        }

        TrapAlertDto structuredTrap = resolveStructuredTrap(request.trapAlert(), nested, null);
        String trapText = resolveTrapText(request.trap(), structuredTrap);
        if (trapText == null || trapText.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trap description cannot be empty");
        }

        String lessonType = (request.lessonType() != null && !request.lessonType().isBlank())
                ? request.lessonType().trim() : "CONCEPT";
        boolean formulaRequired = blueprintAgent.isFormulaSheetRequired(
                taxonomy.subject(), taxonomy.topic(), taxonomy.subtopic(), lessonType);

        List<FormulaEntryDto> structuredFormulas = resolveStructuredFormulas(request.formulaCheatSheet(), nested, null);
        List<String> cheatSheetStrings = resolveCheatSheetStrings(request.cheatSheet(), structuredFormulas);
        if (formulaRequired && cheatSheetStrings.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cheat sheet must contain at least 1 formula or framework for Math, Science, or Numerical topics"
            );
        }

        List<String> takeaways = resolveKeyTakeaways(request.keyTakeaways(), nested, null);
        List<QuickCheckItemDto> quickChecks = resolveQuickChecks(request.quickCheckItems(), nested, null);

        StepByStepBreakdownDto breakdown = buildNormalizedBreakdown(new BreakdownNormalizationContext(
                request.stepByStepBreakdown(),
                resolveNestedSteps(nested, null),
                firstNonBlank(request.problemStatement(), nested != null ? nested.problemStatement() : null),
                firstNonBlank(request.finalAnswer(), nested != null ? nested.finalAnswer() : null),
                taxonomy.subtopic(),
                structuredTrap,
                structuredFormulas,
                takeaways,
                quickChecks
        ));

        validateContentKaTex(coreConcept, trapText, cheatSheetStrings, breakdown, formulaRequired);

        ManualLessonContent lessonContent = new ManualLessonContent(coreConcept, trapText, cheatSheetStrings, breakdown, lessonType);
        SubtopicMicroLesson entity = buildManualLessonEntity(request, taxonomy, authorId, lessonContent);
        SubtopicMicroLesson saved = repository.save(entity);
        MicroLessonResponse response = toResponse(saved);

        updateCacheOnStatus(saved.getSubtopicId(), response, saved.getStatus());
        return response;
    }

    private TrapAlertDto resolveStructuredTrap(TrapAlertDto direct, MicroLessonStructuredContent nested, StepByStepBreakdownDto existing) {
        if (direct != null) {
            return direct;
        }
        if (nested != null && nested.trapAlert() != null) {
            return nested.trapAlert();
        }
        return existing != null ? existing.structuredTrapAlert() : null;
    }

    private List<FormulaEntryDto> resolveStructuredFormulas(List<FormulaEntryDto> direct, MicroLessonStructuredContent nested, StepByStepBreakdownDto existing) {
        if (direct != null && !direct.isEmpty()) {
            return direct;
        }
        if (nested != null && nested.formulaCheatSheet() != null && !nested.formulaCheatSheet().isEmpty()) {
            return nested.formulaCheatSheet();
        }
        return existing != null ? existing.structuredFormulas() : null;
    }

    private List<String> resolveKeyTakeaways(List<String> direct, MicroLessonStructuredContent nested, StepByStepBreakdownDto existing) {
        if (direct != null && !direct.isEmpty()) {
            return direct;
        }
        if (nested != null && nested.keyTakeaways() != null && !nested.keyTakeaways().isEmpty()) {
            return nested.keyTakeaways();
        }
        return existing != null ? existing.keyTakeaways() : null;
    }

    private List<QuickCheckItemDto> resolveQuickChecks(List<QuickCheckItemDto> direct, MicroLessonStructuredContent nested, StepByStepBreakdownDto existing) {
        if (direct != null && !direct.isEmpty()) {
            return direct;
        }
        if (nested != null && nested.quickCheckItems() != null && !nested.quickCheckItems().isEmpty()) {
            return nested.quickCheckItems();
        }
        return existing != null ? existing.quickCheckItems() : null;
    }

    private List<StepItemDto> resolveNestedSteps(MicroLessonStructuredContent nested, StepByStepBreakdownDto baseBreakdown) {
        if (nested != null && nested.stepByStepBreakdown() != null && !nested.stepByStepBreakdown().isEmpty()) {
            return nested.stepByStepBreakdown();
        }
        return baseBreakdown != null ? baseBreakdown.structuredSteps() : null;
    }

    private void validateContentKaTex(String coreConcept, String trapText, List<String> cheatSheetStrings, StepByStepBreakdownDto breakdown, boolean formulaRequired) {
        MicroLessonContentDto contentToValidate = new MicroLessonContentDto(coreConcept, trapText, cheatSheetStrings, breakdown);
        List<String> validationErrors = kaTexValidator.validateContent(contentToValidate, formulaRequired);
        if (!validationErrors.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "KaTeX validation failed: " + String.join(", ", validationErrors)
            );
        }
    }

    private record ManualLessonContent(
            String coreConcept,
            String trapText,
            List<String> cheatSheetStrings,
            StepByStepBreakdownDto breakdown,
            String lessonType
    ) {}

    private SubtopicMicroLesson buildManualLessonEntity(
            CreateMicroLessonRequest request,
            ResolvedTaxonomy taxonomy,
            UUID authorId,
            ManualLessonContent content) {

        String targetStatus = (request.status() != null && !request.status().isBlank())
                ? resolveStatus(request.status())
                : MicroLessonStatus.APPROVED.name();

        int nextVersion = repository.findTopBySubtopicIdOrderByContentVersionDesc(taxonomy.subtopicId())
                .map(prev -> prev.getContentVersion() + 1)
                .orElse(1);

        String cheatSheetJson;
        String stepByStepJson;
        try {
            cheatSheetJson = objectMapper.writeValueAsString(content.cheatSheetStrings());
            stepByStepJson = objectMapper.writeValueAsString(content.breakdown());
        } catch (JsonProcessingException _) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to serialize micro-lesson content");
        }

        String difficultyLevel = (request.difficultyLevel() != null && !request.difficultyLevel().isBlank())
                ? normalizeDifficulty(request.difficultyLevel().trim()) : "CORE";
        int seqNo = request.sequenceNo() != null ? request.sequenceNo() : 1;
        int estMins = request.estimatedMinutes() != null ? request.estimatedMinutes() : 5;
        String lang = (request.language() != null && !request.language().isBlank())
                ? request.language().trim() : "en";

        return SubtopicMicroLesson.builder()
                .subjectId(taxonomy.subjectId())
                .topicId(taxonomy.topicId())
                .subtopicId(taxonomy.subtopicId())
                .coreConcept(content.coreConcept())
                .trap(content.trapText())
                .cheatSheetJson(cheatSheetJson)
                .stepByStepBreakdownJson(stepByStepJson)
                .status(targetStatus)
                .reviewedBy(authorId)
                .reviewedAt(Instant.now())
                .reviewComments(request.reviewComments())
                .contentVersion(nextVersion)
                .lessonType(content.lessonType())
                .difficultyLevel(difficultyLevel)
                .sequenceNo(seqNo)
                .prerequisiteLessonId(request.prerequisiteLessonId())
                .estimatedMinutes(estMins)
                .language(lang)
                .qualityScore(request.qualityScore())
                .build();
    }

    private void updateCacheOnStatus(Long subtopicId, MicroLessonResponse response, String status) {
        if (MicroLessonStatus.PUBLISHED.name().equals(status)) {
            cacheService.put(subtopicId, response);
        } else {
            cacheService.evict(subtopicId);
        }
    }

    @Transactional
    public MicroLessonResponse updateMicroLesson(UUID id, UpdateMicroLessonRequest request, UUID editorId) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request payload cannot be null");
        }

        SubtopicMicroLesson lesson = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ERR_MICRO_LESSON_NOT_FOUND));

        updateTaxonomyIfProvided(lesson, request);
        applyMetadataUpdates(lesson, request);

        MicroLessonStructuredContent nested = request.content();
        applyCoreConceptAndTrapUpdates(lesson, request, nested);

        List<String> existingCheatSheet = parseCheatSheetJson(lesson.getCheatSheetJson());
        StepByStepBreakdownDto existingBreakdown = parseBreakdownJson(lesson.getStepByStepBreakdownJson());

        List<FormulaEntryDto> structuredFormulas = resolveStructuredFormulas(request.formulaCheatSheet(), nested, existingBreakdown);
        List<String> cheatSheetStrings = resolveUpdatedCheatSheet(request, nested, structuredFormulas, existingCheatSheet);

        StepByStepBreakdownDto mergedBreakdown = buildMergedBreakdown(lesson, request, nested, structuredFormulas, existingBreakdown);

        String subtopicName = resolveSubtopicName(lesson, request.subtopic());
        String topicName = resolveTopicName(lesson, request.topic());
        String subjectName = resolveSubjectName(lesson, request.subject());
        boolean formulaRequired = blueprintAgent.isFormulaSheetRequired(
                subjectName, topicName, subtopicName, lesson.getLessonType());

        validateContentKaTex(lesson.getCoreConcept(), lesson.getTrap(), cheatSheetStrings, mergedBreakdown, formulaRequired);

        serializeAndSaveUpdatedContent(lesson, cheatSheetStrings, mergedBreakdown);
        applyVersionAndReview(lesson, request, editorId);

        SubtopicMicroLesson saved = repository.save(lesson);
        MicroLessonResponse response = toResponse(saved);
        updateCacheOnStatus(saved.getSubtopicId(), response, saved.getStatus());
        return response;
    }

    private void applyMetadataUpdates(SubtopicMicroLesson lesson, UpdateMicroLessonRequest request) {
        if (request.lessonType() != null && !request.lessonType().isBlank()) {
            lesson.setLessonType(request.lessonType().trim());
        }
        if (request.difficultyLevel() != null && !request.difficultyLevel().isBlank()) {
            lesson.setDifficultyLevel(normalizeDifficulty(request.difficultyLevel().trim()));
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
    }

    private void applyCoreConceptAndTrapUpdates(SubtopicMicroLesson lesson, UpdateMicroLessonRequest request, MicroLessonStructuredContent nested) {
        String updatedCore = firstNonBlank(request.coreConcept(), nested != null ? nested.coreConcept() : null);
        if (updatedCore != null) {
            lesson.setCoreConcept(updatedCore);
        }
        TrapAlertDto trapAlert = resolveStructuredTrap(request.trapAlert(), nested, null);
        String updatedTrap = resolveTrapText(request.trap(), trapAlert);
        if (updatedTrap != null && !updatedTrap.isBlank()) {
            lesson.setTrap(updatedTrap);
        }
    }

    private List<String> resolveUpdatedCheatSheet(
            UpdateMicroLessonRequest request,
            MicroLessonStructuredContent nested,
            List<FormulaEntryDto> structuredFormulas,
            List<String> existingCheatSheet) {
        boolean explicitCheatSheetUpdate = request.formulaCheatSheet() != null
                || request.cheatSheet() != null
                || (nested != null && nested.formulaCheatSheet() != null);
        if (explicitCheatSheetUpdate) {
            return resolveCheatSheetStrings(request.cheatSheet(), structuredFormulas);
        }
        return existingCheatSheet;
    }

    private StepByStepBreakdownDto buildMergedBreakdown(
            SubtopicMicroLesson lesson,
            UpdateMicroLessonRequest request,
            MicroLessonStructuredContent nested,
            List<FormulaEntryDto> structuredFormulas,
            StepByStepBreakdownDto existingBreakdown) {

        TrapAlertDto structuredTrap = resolveStructuredTrap(request.trapAlert(), nested, existingBreakdown);
        List<String> takeaways = resolveKeyTakeaways(request.keyTakeaways(), nested, existingBreakdown);
        List<QuickCheckItemDto> quickChecks = resolveQuickChecks(request.quickCheckItems(), nested, existingBreakdown);

        StepByStepBreakdownDto baseBreakdown = request.stepByStepBreakdown() != null ? request.stepByStepBreakdown() : existingBreakdown;
        List<StepItemDto> nestedSteps = resolveNestedSteps(nested, baseBreakdown);

        String subtopicName = resolveSubtopicName(lesson, request.subtopic());
        String problemStatement = firstNonBlank(
                request.problemStatement(),
                nested != null ? nested.problemStatement() : null,
                baseBreakdown != null ? baseBreakdown.questionStem() : null
        );
        String finalAnswer = firstNonBlank(
                request.finalAnswer(),
                nested != null ? nested.finalAnswer() : null,
                baseBreakdown != null ? baseBreakdown.finalAnswer() : null
        );

        return buildNormalizedBreakdown(new BreakdownNormalizationContext(
                baseBreakdown,
                nestedSteps,
                problemStatement,
                finalAnswer,
                subtopicName,
                structuredTrap,
                structuredFormulas,
                takeaways,
                quickChecks
        ));
    }

    private String resolveSubtopicName(SubtopicMicroLesson lesson, String fallback) {
        String dbName = (subtopicRepository != null && lesson.getSubtopicId() != null)
                ? subtopicRepository.findById(lesson.getSubtopicId()).map(Subtopic::getName).orElse(null)
                : null;
        return firstNonBlank(fallback, dbName, DEFAULT_GENERAL);
    }

    private String resolveTopicName(SubtopicMicroLesson lesson, String fallback) {
        String dbName = (topicRepository != null && lesson.getTopicId() != null)
                ? topicRepository.findById(lesson.getTopicId()).map(Topic::getName).orElse(null)
                : null;
        return firstNonBlank(fallback, dbName);
    }

    private String resolveSubjectName(SubtopicMicroLesson lesson, String fallback) {
        String dbName = (subjectRepository != null && lesson.getSubjectId() != null)
                ? subjectRepository.findById(lesson.getSubjectId()).map(Subject::getName).orElse(null)
                : null;
        return firstNonBlank(fallback, dbName);
    }

    private void serializeAndSaveUpdatedContent(SubtopicMicroLesson lesson, List<String> cheatSheetStrings, StepByStepBreakdownDto mergedBreakdown) {
        try {
            lesson.setCheatSheetJson(objectMapper.writeValueAsString(cheatSheetStrings));
            lesson.setStepByStepBreakdownJson(objectMapper.writeValueAsString(mergedBreakdown));
        } catch (JsonProcessingException _) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to serialize updated micro-lesson content");
        }
    }

    private void applyVersionAndReview(SubtopicMicroLesson lesson, UpdateMicroLessonRequest request, UUID editorId) {
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
    }

    @Transactional
    public void deleteMicroLesson(UUID id) {
        SubtopicMicroLesson lesson = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ERR_MICRO_LESSON_NOT_FOUND));
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ERR_MICRO_LESSON_NOT_FOUND));

        if (request == null || request.status() == null || request.status().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required for review transition");
        }

        String rawStatus = request.status().trim().toUpperCase();
        String targetStatus = resolveReviewTargetStatus(rawStatus, lesson.getStatus());

        applyReviewContentEdits(lesson, request);

        List<String> cheatSheet = parseCheatSheetJson(lesson.getCheatSheetJson());
        StepByStepBreakdownDto breakdown = parseBreakdownJson(lesson.getStepByStepBreakdownJson());

        String reviewSubject = resolveSubjectName(lesson, null);
        String reviewTopic = resolveTopicName(lesson, null);
        String reviewSubtopic = resolveSubtopicName(lesson, null);
        boolean formulaRequired = (reviewSubject != null || reviewTopic != null || reviewSubtopic != null)
                && blueprintAgent.isFormulaSheetRequired(reviewSubject, reviewTopic, reviewSubtopic, lesson.getLessonType());

        validateContentKaTex(lesson.getCoreConcept(), lesson.getTrap(), cheatSheet, breakdown, formulaRequired);

        lesson.setStatus(targetStatus);
        lesson.setReviewedBy(reviewerId);
        lesson.setReviewedAt(Instant.now());
        if (request.comments() != null) {
            lesson.setReviewComments(request.comments());
        }

        SubtopicMicroLesson saved = repository.save(lesson);
        MicroLessonResponse response = toResponse(saved);
        updateCacheOnStatus(saved.getSubtopicId(), response, targetStatus);
        return response;
    }

    private String resolveReviewTargetStatus(String rawStatus, String currentStatus) {
        if ("EDIT".equals(rawStatus)) {
            return currentStatus != null ? currentStatus : MicroLessonStatus.IN_REVIEW.name();
        }
        return resolveStatus(rawStatus);
    }

    private void applyReviewContentEdits(SubtopicMicroLesson lesson, ReviewMicroLessonRequest request) {
        if (request.coreConcept() != null && !request.coreConcept().isBlank()) {
            lesson.setCoreConcept(request.coreConcept());
        }
        if (request.trap() != null && !request.trap().isBlank()) {
            lesson.setTrap(request.trap());
        }
        if (request.cheatSheet() != null) {
            try {
                lesson.setCheatSheetJson(objectMapper.writeValueAsString(request.cheatSheet()));
            } catch (JsonProcessingException _) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cheatSheet format");
            }
        }
        if (request.stepByStepBreakdown() != null) {
            try {
                lesson.setStepByStepBreakdownJson(objectMapper.writeValueAsString(request.stepByStepBreakdown()));
            } catch (JsonProcessingException _) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid stepByStepBreakdown format");
            }
        }
    }

    @Transactional
    public MicroLessonResponse generateSingleMicroLesson(GenerateMicroLessonRequest request) {
        if (request == null || request.subtopic() == null || request.subtopic().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subtopic is required for generation");
        }
        Long subjectId = request.subjectId() != null ? request.subjectId() : 1L;
        String subject = (request.subject() != null && !request.subject().isBlank()) ? request.subject() : DEFAULT_GENERAL;
        Long topicId = request.topicId() != null ? request.topicId() : 1L;
        String topic = (request.topic() != null && !request.topic().isBlank()) ? request.topic() : DEFAULT_GENERAL;
        Long subtopicId = request.subtopicId() != null ? request.subtopicId() : (long) (request.subtopic().hashCode() & 0x7FFFFFFF);
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

        ResolvedSubject resolvedSubject = resolveSubject(subjectId, subjectName);
        ResolvedTopic resolvedTopic = resolveTopic(topicId, topicName, resolvedSubject.id());
        ResolvedSubtopic resolvedSubtopic = resolveSubtopic(subtopicId, subtopicName, resolvedTopic.id());

        return new ResolvedTaxonomy(
                resolvedSubject.id(),
                resolvedSubject.name(),
                resolvedTopic.id(),
                resolvedTopic.name(),
                resolvedSubtopic.id(),
                resolvedSubtopic.name()
        );
    }

    private record ResolvedSubject(Long id, String name) {}
    private record ResolvedTopic(Long id, String name) {}
    private record ResolvedSubtopic(Long id, String name) {}

    private ResolvedSubject resolveSubject(Long subjectId, String subjectName) {
        String resName = (subjectName != null && !subjectName.isBlank()) ? subjectName.trim() : null;

        if (resName == null && subjectRepository != null && subjectId != null) {
            Subject sub = subjectRepository.findById(subjectId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject not found: " + subjectId));
            resName = sub.getName();
        }
        if (resName == null || resName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject is required");
        }
        Long resId = subjectId != null ? subjectId : 1L;
        return new ResolvedSubject(resId, resName);
    }

    private ResolvedTopic resolveTopic(Long topicId, String topicName, Long subjectId) {
        String resName = (topicName != null && !topicName.isBlank()) ? topicName.trim() : null;

        if (resName == null && topicRepository != null && topicId != null) {
            Topic top = topicRepository.findById(topicId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic not found: " + topicId));
            if (top.getSubjectId() != null && subjectId != null && !top.getSubjectId().equals(subjectId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic does not belong to specified subject");
            }
            resName = top.getName();
        }
        if (resName == null || resName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic is required");
        }
        Long resId = topicId != null ? topicId : 1L;
        return new ResolvedTopic(resId, resName);
    }

    private String resolveSubtopicNameFromRepo(Long subtopicId, Long expectedTopicId) {
        if (subtopicRepository == null || subtopicId == null) {
            return null;
        }
        Subtopic st = subtopicRepository.findById(subtopicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subtopic not found: " + subtopicId));
        if (st.getTopicId() != null && expectedTopicId != null && !st.getTopicId().equals(expectedTopicId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subtopic does not belong to specified topic");
        }
        return st.getName();
    }

    private static long computeSubtopicFallbackId(String subtopicName) {
        long hash = subtopicName.toLowerCase().trim().hashCode() & 0x7FFFFFFFL;
        return hash > 0 ? hash : 1L;
    }

    private ResolvedSubtopic resolveSubtopic(Long subtopicId, String subtopicName, Long topicId) {
        String resName = (subtopicName != null && !subtopicName.isBlank()) ? subtopicName.trim() : null;
        if (resName == null) {
            resName = resolveSubtopicNameFromRepo(subtopicId, topicId);
        }
        if (resName == null || resName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subtopic is required");
        }
        long resId = subtopicId != null ? subtopicId : computeSubtopicFallbackId(resName);
        return new ResolvedSubtopic(resId, resName);
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
                } catch (IllegalArgumentException _) {
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
        if (formulaEntries != null && !formulaEntries.isEmpty()) {
            return formulaEntries.stream()
                    .filter(Objects::nonNull)
                    .map(this::formatFormulaEntry)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        if (rawCheatSheet != null) {
            return rawCheatSheet.stream()
                    .filter(item -> item != null && !item.isBlank())
                    .map(String::trim)
                    .toList();
        }
        return Collections.emptyList();
    }

    private String formatFormulaEntry(FormulaEntryDto f) {
        String name = f.name() != null ? f.name().trim() : "";
        String latex = f.latex() != null ? f.latex().trim() : "";
        if (!name.isEmpty() && !latex.isEmpty()) {
            return name + ": " + latex;
        }
        if (!latex.isEmpty()) {
            return latex;
        }
        return name;
    }

    private record BreakdownNormalizationContext(
            StepByStepBreakdownDto breakdown,
            List<StepItemDto> fallbackStructuredSteps,
            String fallbackQuestionStem,
            String fallbackFinalAnswer,
            String subtopicName,
            TrapAlertDto structuredTrap,
            List<FormulaEntryDto> structuredFormulas,
            List<String> keyTakeaways,
            List<QuickCheckItemDto> quickCheckItems
    ) {}

    private StepByStepBreakdownDto buildNormalizedBreakdown(BreakdownNormalizationContext ctx) {
        List<StepItemDto> structuredSteps = (ctx.breakdown() != null && ctx.breakdown().structuredSteps() != null && !ctx.breakdown().structuredSteps().isEmpty())
                ? ctx.breakdown().structuredSteps()
                : ctx.fallbackStructuredSteps();

        List<String> rawSteps = buildRawSteps(ctx.breakdown(), structuredSteps);
        String stem = resolveQuestionStem(ctx.breakdown(), ctx.fallbackQuestionStem(), ctx.subtopicName(), rawSteps);
        String finalAns = resolveFinalAnswer(ctx.breakdown(), ctx.fallbackFinalAnswer(), rawSteps, structuredSteps);

        return new StepByStepBreakdownDto(
                stem,
                rawSteps,
                finalAns,
                structuredSteps,
                ctx.structuredTrap(),
                ctx.structuredFormulas(),
                ctx.keyTakeaways(),
                ctx.quickCheckItems()
        );
    }

    private List<String> buildRawSteps(StepByStepBreakdownDto breakdown, List<StepItemDto> structuredSteps) {
        if (breakdown != null && breakdown.steps() != null && !breakdown.steps().isEmpty()) {
            return breakdown.steps().stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(String::trim)
                    .toList();
        }
        if (structuredSteps != null && !structuredSteps.isEmpty()) {
            return structuredSteps.stream()
                    .filter(Objects::nonNull)
                    .map(this::formatStepItem)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        return Collections.emptyList();
    }

    private String formatStepItem(StepItemDto item) {
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
        return sb.toString();
    }

    private String resolveQuestionStem(StepByStepBreakdownDto breakdown, String fallback, String subtopicName, List<String> rawSteps) {
        String stem = firstNonBlank(breakdown != null ? breakdown.questionStem() : null, fallback);
        if ((stem == null || stem.isBlank()) && !rawSteps.isEmpty()) {
            return "Worked Example: " + (subtopicName != null ? subtopicName : "Concept Application");
        }
        return stem;
    }

    private String resolveFinalAnswer(StepByStepBreakdownDto breakdown, String fallback, List<String> rawSteps, List<StepItemDto> structuredSteps) {
        String finalAns = firstNonBlank(breakdown != null ? breakdown.finalAnswer() : null, fallback);
        if ((finalAns == null || finalAns.isBlank()) && !rawSteps.isEmpty()) {
            if (structuredSteps != null && !structuredSteps.isEmpty()) {
                StepItemDto last = structuredSteps.getLast();
                return firstNonBlank(last.tip(), last.formula(), last.explanation(), "Verified solution");
            }
            return rawSteps.getLast();
        }
        return finalAns;
    }

    private List<String> parseCheatSheetJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException _) {
            return Collections.emptyList();
        }
    }

    private StepByStepBreakdownDto parseBreakdownJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, StepByStepBreakdownDto.class);
        } catch (JsonProcessingException _) {
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

        Map<Long, String> subjectNames = loadSubjectNames(subjectIds);
        Map<Long, String> topicNames = loadTopicNames(topicIds);
        Map<Long, String> subtopicNames = loadSubtopicNames(subtopicIds);

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

    private Map<Long, String> loadSubjectNames(Set<Long> ids) {
        Map<Long, String> result = new HashMap<>();
        if (subjectRepository != null && !ids.isEmpty()) {
            try {
                subjectRepository.findAllById(ids).forEach(s -> result.put(s.getId(), s.getName()));
            } catch (Exception _) {
                // Ignore failure on optional name loading
            }
        }
        return result;
    }

    private Map<Long, String> loadTopicNames(Set<Long> ids) {
        Map<Long, String> result = new HashMap<>();
        if (topicRepository != null && !ids.isEmpty()) {
            try {
                topicRepository.findAllById(ids).forEach(t -> result.put(t.getId(), t.getName()));
            } catch (Exception _) {
                // Ignore failure on optional name loading
            }
        }
        return result;
    }

    private Map<Long, String> loadSubtopicNames(Set<Long> ids) {
        Map<Long, String> result = new HashMap<>();
        if (subtopicRepository != null && !ids.isEmpty()) {
            try {
                subtopicRepository.findAllById(ids).forEach(st -> result.put(st.getId(), st.getName()));
            } catch (Exception _) {
                // Ignore failure on optional name loading
            }
        }
        return result;
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
                    normalizeDifficulty(entity.getDifficultyLevel()),
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
                normalizeDifficulty(base.difficultyLevel()),
                base.sequenceNo(),
                base.prerequisiteLessonId(),
                base.estimatedMinutes(),
                base.qualityScore()
        );
    }

    private MicroLessonResponse toResponse(SubtopicMicroLesson entity) {
        if (entity == null) return null;

        String sName = null;
        String tName = null;
        String stName = null;
        if (subtopicRepository != null && entity.getSubtopicId() != null) {
            try {
                stName = subtopicRepository.findById(entity.getSubtopicId()).map(Subtopic::getName).orElse(null);
            } catch (Exception _) {
                // Ignore subtopic lookup failure
            }
        }
        if (topicRepository != null && entity.getTopicId() != null) {
            try {
                tName = topicRepository.findById(entity.getTopicId()).map(Topic::getName).orElse(null);
            } catch (Exception _) {
                // Ignore topic lookup failure
            }
        }
        if (subjectRepository != null && entity.getSubjectId() != null) {
            try {
                sName = subjectRepository.findById(entity.getSubjectId()).map(Subject::getName).orElse(null);
            } catch (Exception _) {
                // Ignore subject lookup failure
            }
        }

        MicroLessonResponse base = (generatorService != null) ? generatorService.toResponse(entity) : null;
        return enrichWithTaxonomy(base, entity, sName, tName, stName);
    }

    /**
     * Normalizes difficulty string to canonical pedagogical levels (FOUNDATION, CORE, ADVANCED).
     */
    public static String normalizeDifficulty(String diff) {
        if (diff == null || diff.isBlank()) return "CORE";
        return switch (diff.trim().toUpperCase()) {
            case "FOUNDATION", "EASY", "BEGINNER", "BASIC" -> "FOUNDATION";
            case "CORE", "MEDIUM", "INTERMEDIATE", "STANDARD" -> "CORE";
            case "ADVANCED", "HARD", "EXPERT", "OLYMPIAD" -> "ADVANCED";
            default -> diff.trim().toUpperCase();
        };
    }

    private boolean difficultyMatches(String requestedDifficulty, String lessonDifficulty) {
        if (requestedDifficulty == null || requestedDifficulty.isBlank()) {
            return true;
        }
        if (lessonDifficulty == null || lessonDifficulty.isBlank()) {
            return false;
        }
        if (requestedDifficulty.equalsIgnoreCase(lessonDifficulty)) {
            return true;
        }
        String normReq = normalizeDifficulty(requestedDifficulty);
        String normLesson = normalizeDifficulty(lessonDifficulty);
        return normReq.equalsIgnoreCase(normLesson);
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

        Optional<MicroLessonResponse> baseOpt = self.getPublishedMicroLesson(
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
        return self.getPublishedMicroLesson(subtopicId, targetExam, null, null, null);
    }

    @Transactional
    public Optional<MicroLessonResponse> getPublishedMicroLesson(
            Long subtopicId,
            String targetExam,
            String subjectName,
            String topicName,
            String subtopicName) {

        Optional<MicroLessonResponse> existing = findExistingPublishedLesson(subtopicId);
        if (existing.isPresent()) {
            return existing;
        }

        String exam = (targetExam != null && !targetExam.isBlank()) ? targetExam : "JEE_MAIN";
        ResolvedTaxonomyContext taxCtx = resolveTaxonomyContext(subtopicId, subjectName, topicName, subtopicName);

        if (taxCtx.subtopic() == null || taxCtx.subtopic().isBlank()) {
            return Optional.empty();
        }

        return generateAndCacheLesson(taxCtx, exam);
    }

    private Optional<MicroLessonResponse> findExistingPublishedLesson(Long subtopicId) {
        if (subtopicId == null || subtopicId <= 0) {
            return Optional.empty();
        }
        Optional<MicroLessonResponse> cached = cacheService.getCached(subtopicId);
        if (cached.isPresent()) {
            return cached;
        }

        Optional<SubtopicMicroLesson> publishedLesson = repository
                .findFirstBySubtopicIdAndStatusOrderByContentVersionDesc(
                        subtopicId, MicroLessonStatus.PUBLISHED.name());
        if (publishedLesson.isPresent()) {
            MicroLessonResponse response = toResponse(publishedLesson.get());
            cacheService.put(subtopicId, response);
            return Optional.of(response);
        }

        Optional<SubtopicMicroLesson> anyLesson = repository
                .findTopBySubtopicIdOrderByContentVersionDesc(subtopicId);
        return anyLesson.map(this::toResponse);
    }

    private record ResolvedTaxonomyContext(
            Long subjectId,
            String subject,
            Long topicId,
            String topic,
            Long subtopicId,
            String subtopic
    ) {}

    private record SubtopicTaxonomyResolution(Long subjectId, String subject, Long topicId, String topic, String subtopic) {}

    private SubtopicTaxonomyResolution resolveTaxonomyFromSubtopicRepo(Long subtopicId, String currentSubject, String currentTopic, String currentSubtopic) {
        if (subtopicRepository == null || subtopicId == null) {
            return new SubtopicTaxonomyResolution(1L, currentSubject, 1L, currentTopic, currentSubtopic);
        }
        try {
            Optional<Subtopic> stOpt = subtopicRepository.findById(subtopicId);
            if (stOpt.isEmpty()) {
                return new SubtopicTaxonomyResolution(1L, currentSubject, 1L, currentTopic, currentSubtopic);
            }
            Subtopic st = stOpt.get();
            String subtopic = currentSubtopic != null ? currentSubtopic : st.getName();
            if (st.getTopicId() == null) {
                return new SubtopicTaxonomyResolution(1L, currentSubject, 1L, currentTopic, subtopic);
            }
            ResolvedHierarchy hierarchy = loadParentHierarchy(st.getTopicId(), currentSubject, currentTopic);
            return new SubtopicTaxonomyResolution(hierarchy.subjectId(), hierarchy.subject(), st.getTopicId(), hierarchy.topic(), subtopic);
        } catch (Exception ex) {
            log.debug("Could not resolve subtopic from DB repository for ID {}: {}", subtopicId, ex.getMessage());
            return new SubtopicTaxonomyResolution(1L, currentSubject, 1L, currentTopic, currentSubtopic);
        }
    }

    private ResolvedTaxonomyContext resolveTaxonomyContext(Long subtopicId, String subjectName, String topicName, String subtopicName) {
        String resSubject = (subjectName != null && !subjectName.isBlank()) ? subjectName.trim() : null;
        String resTopic = (topicName != null && !topicName.isBlank()) ? topicName.trim() : null;
        String resSubtopic = (subtopicName != null && !subtopicName.isBlank()) ? subtopicName.trim() : null;
        Long effectiveSubtopicId = (subtopicId != null && subtopicId > 0) ? subtopicId : null;

        SubtopicTaxonomyResolution res = resolveTaxonomyFromSubtopicRepo(effectiveSubtopicId, resSubject, resTopic, resSubtopic);
        Long resSubId = res.subjectId();
        resSubject = res.subject();
        Long resTopicId = res.topicId();
        resTopic = res.topic();
        resSubtopic = res.subtopic();

        if (resSubtopic == null || resSubtopic.isBlank()) {
            return new ResolvedTaxonomyContext(resSubId, resSubject, resTopicId, resTopic, effectiveSubtopicId, null);
        }

        if (effectiveSubtopicId == null) {
            effectiveSubtopicId = computeSubtopicFallbackId(resSubtopic);
        }

        resSubject = firstNonBlank(resSubject, DEFAULT_GENERAL);
        resTopic = firstNonBlank(resTopic, DEFAULT_GENERAL);

        return new ResolvedTaxonomyContext(resSubId, resSubject, resTopicId, resTopic, effectiveSubtopicId, resSubtopic);
    }

    private record ResolvedHierarchy(Long subjectId, String subject, String topic) {}

    private String resolveSubjectNameById(Long subjectId) {
        if (subjectRepository == null || subjectId == null) {
            return null;
        }
        return subjectRepository.findById(subjectId)
                .map(Subject::getName)
                .orElse(null);
    }

    private ResolvedHierarchy loadParentHierarchy(Long topicId, String existingSubject, String existingTopic) {
        if (topicRepository == null || topicId == null) {
            return new ResolvedHierarchy(1L, existingSubject, existingTopic);
        }
        Optional<Topic> topOpt = topicRepository.findById(topicId);
        if (topOpt.isEmpty()) {
            return new ResolvedHierarchy(1L, existingSubject, existingTopic);
        }
        Topic top = topOpt.get();
        String topName = existingTopic != null ? existingTopic : top.getName();
        Long subId = top.getSubjectId() != null ? top.getSubjectId() : 1L;
        String subj = existingSubject != null ? existingSubject : resolveSubjectNameById(top.getSubjectId());
        return new ResolvedHierarchy(subId, subj, topName);
    }

    private Optional<MicroLessonResponse> generateAndCacheLesson(ResolvedTaxonomyContext tax, String exam) {
        try {
            SubtopicMicroLesson generated = generatorService.generateForSubtopic(
                    tax.subjectId(),
                    tax.subject(),
                    tax.topicId(),
                    tax.topic(),
                    tax.subtopicId(),
                    tax.subtopic(),
                    exam
            );
            MicroLessonResponse response = toResponse(generated);
            cacheService.put(tax.subtopicId(), response);
            return Optional.of(response);
        } catch (Exception ex) {
            log.warn("On-demand synthesis failed for subtopic {}: {}. Generating fallback content.",
                    tax.subtopicId(), ex.getMessage());
            return buildFallbackLessonResponse(tax, exam);
        }
    }

    private Optional<MicroLessonResponse> buildFallbackLessonResponse(ResolvedTaxonomyContext tax, String exam) {
        try {
            MicroLessonContentDto fallbackContent = generatorService.buildFallbackContent(
                    tax.subject(), tax.topic(), tax.subtopic(), exam);
            SubtopicMicroLesson transientLesson = SubtopicMicroLesson.builder()
                    .subjectId(tax.subjectId())
                    .topicId(tax.topicId())
                    .subtopicId(tax.subtopicId())
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
            if (!matchesFilters(lesson, lessonType, difficulty)) {
                continue;
            }
            MicroLessonResponse res = toResponse(lesson);
            if (res != null) {
                responses.add(localizeResponseIfRequested(res, language));
            }
        }
        return responses;
    }

    private boolean matchesFilters(SubtopicMicroLesson lesson, String lessonType, String difficulty) {
        if (lessonType != null && !lessonType.isBlank() && !lessonType.equalsIgnoreCase(lesson.getLessonType())) {
            return false;
        }
        return difficulty == null || difficulty.isBlank() || difficultyMatches(difficulty, lesson.getDifficultyLevel());
    }

    private MicroLessonResponse localizeResponseIfRequested(MicroLessonResponse res, String language) {
        if (language == null || language.isBlank() || "en".equalsIgnoreCase(language.trim())) {
            return res;
        }
        if (translationWorkflowService != null && res.id() != null) {
            try {
                return translationWorkflowService.getLocalizedMicroLesson(res.id(), language.trim().toLowerCase(), TenantContext.get());
            } catch (Exception _) {
                return res.withLocalization("en", true);
            }
        }
        return res;
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
            self.recordLessonCompletion(request.getLessonId(), request.getPostAccuracyDelta());
        } else {
            self.recordLessonView(request.getLessonId());
        }
    }
}
