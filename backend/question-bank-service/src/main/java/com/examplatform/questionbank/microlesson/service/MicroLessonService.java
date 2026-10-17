// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.service;

import com.examplatform.questionbank.microlesson.ai.KaTexValidator;
import com.examplatform.questionbank.microlesson.ai.MicroLessonGeneratorService;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    public MicroLessonService(
            SubtopicMicroLessonRepository repository,
            MicroLessonGeneratorService generatorService,
            MicroLessonCacheService cacheService,
            KaTexValidator kaTexValidator,
            ObjectMapper objectMapper) {
        this(repository, generatorService, cacheService, kaTexValidator, objectMapper, null, null, null);
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
            @Autowired(required = false) SubjectRepository subjectRepository) {
        this.repository = repository;
        this.generatorService = generatorService;
        this.cacheService = cacheService;
        this.kaTexValidator = kaTexValidator;
        this.objectMapper = objectMapper;
        this.subtopicRepository = subtopicRepository;
        this.topicRepository = topicRepository;
        this.subjectRepository = subjectRepository;
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

        return result.map(generatorService::toResponse);
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

            if (subjectId != null) {
                predicates.add(cb.equal(root.get("subjectId"), subjectId));
            }

            if (subject != null && !subject.isBlank() && !"ALL".equalsIgnoreCase(subject.trim())) {
                predicates.add(cb.equal(cb.lower(root.get("subject")), subject.trim().toLowerCase()));
            }

            if (topic != null && !topic.isBlank() && !"ALL".equalsIgnoreCase(topic.trim())) {
                predicates.add(cb.equal(cb.lower(root.get("topic")), topic.trim().toLowerCase()));
            }

            if (targetExam != null && !targetExam.isBlank() && !"ALL".equalsIgnoreCase(targetExam.trim())) {
                predicates.add(cb.equal(cb.upper(root.get("targetExam")), targetExam.trim().toUpperCase()));
            }

            if (search != null && !search.isBlank()) {
                String term = "%" + search.trim().toLowerCase() + "%";
                Predicate subtopicMatch = cb.like(cb.lower(root.get("subtopic")), term);
                Predicate topicMatch = cb.like(cb.lower(root.get("topic")), term);
                Predicate subjectMatch = cb.like(cb.lower(root.get("subject")), term);
                Predicate coreConceptMatch = cb.like(cb.lower(root.get("coreConcept")), term);
                predicates.add(cb.or(subtopicMatch, topicMatch, subjectMatch, coreConceptMatch));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
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
        List<String> subjects = repository.findDistinctSubjects();
        List<String> topics = repository.findDistinctTopics(subject);
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
                : null;
        if (exam == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target exam is required");
        }

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

        List<FormulaEntryDto> structuredFormulas = request.formulaCheatSheet() != null && !request.formulaCheatSheet().isEmpty()
                ? request.formulaCheatSheet()
                : (nested != null && nested.formulaCheatSheet() != null ? nested.formulaCheatSheet() : null);
        List<String> cheatSheetStrings = resolveCheatSheetStrings(request.cheatSheet(), structuredFormulas);
        if (cheatSheetStrings.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cheat sheet must contain at least 1 formula or framework");
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
        List<String> validationErrors = kaTexValidator.validateContent(contentToValidate);
        if (!validationErrors.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "KaTeX validation failed: " + String.join(", ", validationErrors)
            );
        }

        String targetStatus = (request.status() != null && !request.status().isBlank())
                ? resolveStatus(request.status())
                : MicroLessonStatus.APPROVED.name();

        int nextVersion = repository.findTopBySubtopicIdAndTargetExamOrderByContentVersionDesc(taxonomy.subtopicId(), exam)
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

        SubtopicMicroLesson entity = SubtopicMicroLesson.builder()
                .subjectId(taxonomy.subjectId())
                .topicId(taxonomy.topicId())
                .subtopicId(taxonomy.subtopicId())
                .subject(taxonomy.subject())
                .topic(taxonomy.topic())
                .subtopic(taxonomy.subtopic())
                .targetExam(exam)
                .coreConcept(coreConcept)
                .trap(trapText)
                .cheatSheetJson(cheatSheetJson)
                .stepByStepBreakdownJson(stepByStepJson)
                .status(targetStatus)
                .reviewedBy(authorId)
                .reviewedAt(Instant.now())
                .reviewComments(request.reviewComments())
                .contentVersion(nextVersion)
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

        if (request.targetExam() != null && !request.targetExam().isBlank()) {
            lesson.setTargetExam(request.targetExam().trim());
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

        List<FormulaEntryDto> structuredFormulas = request.formulaCheatSheet() != null && !request.formulaCheatSheet().isEmpty()
                ? request.formulaCheatSheet()
                : (nested != null && nested.formulaCheatSheet() != null && !nested.formulaCheatSheet().isEmpty()
                        ? nested.formulaCheatSheet()
                        : (existingBreakdown != null ? existingBreakdown.structuredFormulas() : null));

        List<String> cheatSheetStrings;
        if (request.cheatSheet() != null && !request.cheatSheet().isEmpty()) {
            cheatSheetStrings = request.cheatSheet();
        } else if (request.formulaCheatSheet() != null && !request.formulaCheatSheet().isEmpty()) {
            cheatSheetStrings = resolveCheatSheetStrings(null, request.formulaCheatSheet());
        } else if (nested != null && nested.formulaCheatSheet() != null && !nested.formulaCheatSheet().isEmpty()) {
            cheatSheetStrings = resolveCheatSheetStrings(null, nested.formulaCheatSheet());
        } else {
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

        StepByStepBreakdownDto mergedBreakdown = buildNormalizedBreakdown(
                baseBreakdown,
                nestedSteps,
                firstNonBlank(request.problemStatement(), nested != null ? nested.problemStatement() : (baseBreakdown != null ? baseBreakdown.questionStem() : null)),
                firstNonBlank(request.finalAnswer(), nested != null ? nested.finalAnswer() : (baseBreakdown != null ? baseBreakdown.finalAnswer() : null)),
                lesson.getSubtopic(),
                structuredTrap,
                structuredFormulas,
                takeaways,
                quickChecks
        );

        MicroLessonContentDto contentToValidate = new MicroLessonContentDto(
                lesson.getCoreConcept(),
                lesson.getTrap(),
                cheatSheetStrings,
                mergedBreakdown
        );
        List<String> validationErrors = kaTexValidator.validateContent(contentToValidate);
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
                repository.findTopBySubtopicIdAndTargetExamOrderByContentVersionDesc(lesson.getSubtopicId(), lesson.getTargetExam())
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

        MicroLessonContentDto contentToValidate = new MicroLessonContentDto(
                lesson.getCoreConcept(),
                lesson.getTrap(),
                cheatSheet,
                breakdown
        );
        List<String> validationErrors = kaTexValidator.validateContent(contentToValidate);
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
            if (request.subject() == null && subjectRepository != null) {
                Subject sub = subjectRepository.findById(request.subjectId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject not found: " + request.subjectId()));
                lesson.setSubject(sub.getName());
            }
        }
        if (request.subject() != null && !request.subject().isBlank()) {
            lesson.setSubject(request.subject().trim());
        }

        if (request.topicId() != null) {
            lesson.setTopicId(request.topicId());
            if (request.topic() == null && topicRepository != null) {
                Topic top = topicRepository.findById(request.topicId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic not found: " + request.topicId()));
                lesson.setTopic(top.getName());
            }
        }
        if (request.topic() != null && !request.topic().isBlank()) {
            lesson.setTopic(request.topic().trim());
        }

        if (request.subtopicId() != null) {
            lesson.setSubtopicId(request.subtopicId());
            if (request.subtopic() == null && subtopicRepository != null) {
                Subtopic st = subtopicRepository.findById(request.subtopicId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subtopic not found: " + request.subtopicId()));
                lesson.setSubtopic(st.getName());
            }
        }
        if (request.subtopic() != null && !request.subtopic().isBlank()) {
            lesson.setSubtopic(request.subtopic().trim());
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

    private MicroLessonResponse toResponse(SubtopicMicroLesson entity) {
        if (generatorService != null) {
            MicroLessonResponse res = generatorService.toResponse(entity);
            if (res != null) {
                return res;
            }
        }
        return new MicroLessonResponse(
                entity.getId(),
                entity.getSubjectId(),
                entity.getTopicId(),
                entity.getSubtopicId(),
                entity.getSubject(),
                entity.getTopic(),
                entity.getSubtopic(),
                entity.getTargetExam(),
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
                entity.getUpdatedAt()
        );
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
                    .findFirstBySubtopicIdAndTargetExamAndStatusOrderByContentVersionDesc(
                            subtopicId, exam, MicroLessonStatus.PUBLISHED.name())
                    .or(() -> repository.findFirstBySubtopicIdAndStatusOrderByContentVersionDesc(
                            subtopicId, MicroLessonStatus.PUBLISHED.name()));
            if (publishedLesson.isPresent()) {
                MicroLessonResponse response = generatorService.toResponse(publishedLesson.get());
                cacheService.put(subtopicId, response);
                return Optional.of(response);
            }

            // 3. Next check if any micro-lesson (AI_GENERATED, PENDING_REVIEW, PUBLISHED) exists
            Optional<SubtopicMicroLesson> anyLesson = repository
                    .findTopBySubtopicIdAndTargetExamOrderByContentVersionDesc(subtopicId, exam)
                    .or(() -> repository.findTopBySubtopicIdOrderByContentVersionDesc(subtopicId));
            if (anyLesson.isPresent()) {
                MicroLessonResponse response = generatorService.toResponse(anyLesson.get());
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
            MicroLessonResponse response = generatorService.toResponse(generated);
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
                        .subject(resolvedSubject)
                        .topic(resolvedTopic)
                        .subtopic(resolvedSubtopic)
                        .targetExam(exam)
                        .coreConcept(fallbackContent.coreConcept())
                        .trap(fallbackContent.trap())
                        .cheatSheetJson(objectMapper.writeValueAsString(fallbackContent.cheatSheet()))
                        .stepByStepBreakdownJson(objectMapper.writeValueAsString(fallbackContent.stepByStepBreakdown()))
                        .status(MicroLessonStatus.AI_GENERATED.name())
                        .contentVersion(1)
                        .build();
                return Optional.of(generatorService.toResponse(transientLesson));
            } catch (Exception e2) {
                log.error("Failed to construct fallback micro-lesson: {}", e2.getMessage());
                return Optional.empty();
            }
        }
    }
}
