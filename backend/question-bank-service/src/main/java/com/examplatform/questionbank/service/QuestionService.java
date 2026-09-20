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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.\n */

package com.examplatform.questionbank.service;

import com.examplatform.questionbank.ai.embedding.EmbeddingService;
import com.examplatform.questionbank.ai.similarity.SimilarityCheckResult;
import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.domain.Subject;
import com.examplatform.questionbank.domain.Subtopic;
import com.examplatform.questionbank.domain.Topic;
import com.examplatform.questionbank.domain.enums.QuestionType;
import com.examplatform.questionbank.dto.CreateQuestionRequest;
import com.examplatform.questionbank.dto.QuestionResponse;
import com.examplatform.questionbank.exception.SimilarQuestionException;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.questionbank.repository.SubjectRepository;
import com.examplatform.questionbank.repository.SubtopicRepository;
import com.examplatform.questionbank.repository.TopicRepository;
import com.examplatform.questionbank.translation.domain.Translation;
import com.examplatform.questionbank.translation.repository.TranslationRepository;
import com.examplatform.questionbank.util.EmbeddingUtils;
import com.examplatform.shared.messaging.EventPublisher;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for Question authoring, lifecycle management, similarity detection,
 * hierarchy resolution, and multi-field smart querying.
 *
 * Validates: Requirements 4.1, 4.2, 4.3, 4.5, 4.6, 5.1, 5.2, 5.3, 5.5, FR-1, FR-2
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class QuestionService {

    private static final String AUDIT_TOPIC = "audit-events";

    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final SubtopicRepository subtopicRepository;
    private final SimilarityDetectionService similarityDetectionService;
    private final EmbeddingService embeddingService;
    private final EventPublisher eventPublisher;
    private final TranslationRepository translationRepository;

    @Value("${app.encryption.enabled:true}")
    private boolean encryptionEnabled;

    /**
     * Resolves and validates the numeric Subject -> Topic -> Subtopic hierarchy.
     */
    public record ResolvedHierarchy(
            Long subjectId, String subjectName,
            Long topicId, String topicName,
            Long subtopicId, String subtopicName
    ) {}

    public ResolvedHierarchy resolveHierarchy(CreateQuestionRequest request, String tenantId) {
        if (request.getSubjectId() == null) {
            throw new IllegalArgumentException("subjectId is required to create a question");
        }
        if (request.getTopicId() == null) {
            throw new IllegalArgumentException("topicId is required to create a question");
        }

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .filter(s -> tenantId.equals(s.getTenantId()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subject not found for id=" + request.getSubjectId() + " in tenant " + tenantId));

        Topic topic = topicRepository.findById(request.getTopicId())
                .filter(t -> tenantId.equals(t.getTenantId()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Topic not found for id=" + request.getTopicId() + " in tenant " + tenantId));

        if (!topic.getSubjectId().equals(subject.getId())) {
            throw new IllegalArgumentException("Topic " + topic.getId()
                    + " does not belong to subject " + subject.getId());
        }

        Long subtopicId = null;
        String subtopicName = null;
        if (request.getSubtopicId() != null) {
            Subtopic subtopic = subtopicRepository.findById(request.getSubtopicId())
                    .filter(st -> tenantId.equals(st.getTenantId()))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Subtopic not found for id=" + request.getSubtopicId() + " in tenant " + tenantId));
            if (!subtopic.getTopicId().equals(topic.getId())) {
                throw new IllegalArgumentException("Subtopic " + subtopic.getId()
                        + " does not belong to topic " + topic.getId());
            }
            subtopicId = subtopic.getId();
            subtopicName = subtopic.getName();
        }

        return new ResolvedHierarchy(
                subject.getId(), subject.getName(),
                topic.getId(), topic.getName(),
                subtopicId, subtopicName);
    }

    /**
     * Creates a new question in DRAFT state with a unique per-question DEK.
     *
     * @param request   validated creation request
     * @param authorId  UUID of the question author (from JWT sub claim)
     * @param tenantId  tenant identifier (from X-Tenant-Id header)
     * @return the created question response with decrypted content
     */
    public QuestionResponse createQuestion(CreateQuestionRequest request, UUID authorId, String tenantId) {
        // Validate question type is in the supported set
        if (request.getQuestionType() == null) {
            throw new IllegalArgumentException("Question type must be one of the supported types: "
                    + "SINGLE_MCQ, MULTI_MCQ, NUMERICAL, DESCRIPTIVE, MATRIX_MATCH, ASSERTION_REASON, CODING, CASE_STUDY");
        }

        // Resolve and validate the Subject -> Topic -> Subtopic hierarchy by numeric id.
        ResolvedHierarchy hierarchy = resolveHierarchy(request, tenantId);

        // Check similarity against existing questions in same subject+tenant (FR-2)
        SimilarityCheckResult similarityResult = null;
        try {
            similarityResult = similarityDetectionService.enforceNoDuplicate(
                    request.getContent(), hierarchy.subjectName(), tenantId);
        } catch (SimilarQuestionException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Similarity check unavailable during question creation. " +
                    "Proceeding without duplicate detection. Reason: {}", e.getMessage());
        }

        // Generate per-question DEK key name only when encryption is enabled
        String dekKeyName = encryptionEnabled ? "question-dek-" + UUID.randomUUID() : null;

        // Validate and serialize options for MCQ/MSQ
        String answerKey = request.getAnswerKey();
        if (request.getOptions() != null && !request.getOptions().isEmpty()) {
            var options = request.getOptions();
            if (options.size() < 2 || options.size() > 5) {
                throw new IllegalArgumentException("MCQ/MSQ questions must have between 2 and 5 options");
            }
            String[] ids = {"A", "B", "C", "D", "E", "F"};
            for (int i = 0; i < options.size(); i++) {
                options.get(i).setId(ids[i]);
            }
            long correctCount = options.stream().filter(o -> o.isCorrect()).count();
            QuestionType questionType = request.getQuestionType();
            if (questionType == QuestionType.SINGLE_MCQ) {
                if (correctCount != 1) {
                    throw new IllegalArgumentException("MCQ questions must have exactly one correct option");
                }
            } else if (questionType == QuestionType.MULTI_MCQ) {
                if (correctCount < 1) {
                    throw new IllegalArgumentException("MSQ questions must have at least one correct option");
                }
            }
            try {
                answerKey = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(options);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize options", e);
            }
        }

        boolean hasImages = detectHasImages(request.getContent(), request.getExplanation(), request.getOptions());

        // Build Question entity
        Question question = Question.builder()
                .subjectId(hierarchy.subjectId())
                .topicId(hierarchy.topicId())
                .subtopicId(hierarchy.subtopicId())
                .subject(hierarchy.subjectName())
                .topic(hierarchy.topicName())
                .subtopic(hierarchy.subtopicName())
                .chapter(request.getChapter())
                .difficulty(request.getDifficulty().name())
                .cognitiveLevel(request.getCognitiveLevel().name())
                .questionType(request.getQuestionType().name())
                .content(request.getContent())
                .answerKey(answerKey)
                .options(request.getOptions())
                .explanation(request.getExplanation())
                .references(request.getReferences())
                .hasImages(hasImages)
                .passageId(request.getPassageId())
                .passageOrderIndex(request.getPassageOrderIndex())
                .state("DRAFT")
                .encryptionKeyId(dekKeyName)
                .authorId(authorId)
                .build();

        // Set tenant context
        question.setTenantId(tenantId);

        // Persist — EncryptedFieldConverter encrypts content/answerKey only when app.encryption.enabled=true
        Question saved = questionRepository.save(question);

        // Generate and store embedding via native query (FR-1)
        try {
            float[] embedding = embeddingService.embed(request.getContent());
            if (embedding != null && embedding.length > 0) {
                questionRepository.updateEmbedding(saved.getId(), EmbeddingUtils.embeddingToString(embedding));
                log.debug("Embedding generated and stored for question: id={}", saved.getId());
            }
        } catch (Exception e) {
            log.warn("Failed to generate embedding for question id={}. " +
                    "Question created without embedding. Reason: {}", saved.getId(), e.getMessage());
        }

        log.info("Question created: id={}, type={}, author={}, tenant={}, encrypted={}",
                saved.getId(), saved.getQuestionType(), authorId, tenantId, encryptionEnabled);

        publishAuditEvent("QUESTION_CREATED", saved.getId(), authorId, tenantId,
                Map.of("questionType", saved.getQuestionType(), "state", saved.getState()));

        QuestionResponse response = toResponse(saved);
        if (similarityResult != null && similarityResult.status() == SimilarityCheckResult.Status.WARN) {
            List<QuestionResponse.SimilarQuestionWarning> warnings = similarityResult.similarQuestions().stream()
                    .<QuestionResponse.SimilarQuestionWarning>map(sq -> QuestionResponse.SimilarQuestionWarning.builder()
                            .questionId(sq.questionId())
                            .similarity(sq.similarity())
                            .contentSnippet(sq.content())
                            .build())
                    .toList();
            response.setWarnings(warnings);
        }

        return response;
    }

    /**
     * Lists questions for a tenant with optional filtering by hierarchy, difficulty, state, text search,
     * target language, translation status, and dynamic sorting.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<QuestionResponse> listQuestions(
            String subject, Long subjectId, String topic, Long topicId,
            String difficulty, String state, String search,
            String targetLang, String translationStatus,
            String sort, String order,
            int page, int size, String tenantId) {

        org.springframework.data.domain.Sort.Direction direction = "asc".equalsIgnoreCase(order)
                ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;
        String sortProperty = resolveQuestionSortProperty(sort);
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(page, size,
                        org.springframework.data.domain.Sort.by(direction, sortProperty));

        org.springframework.data.jpa.domain.Specification<Question> spec =
                org.springframework.data.jpa.domain.Specification.where(tenantEquals(tenantId));

        if (subjectId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("subjectId"), subjectId));
        } else if (subject != null && !subject.isBlank()) {
            try {
                Long parsedId = Long.parseLong(subject.trim());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("subjectId"), parsedId));
            } catch (NumberFormatException e) {
                spec = spec.and(fieldEquals("subject", subject));
            }
        }

        if (topicId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("topicId"), topicId));
        } else if (topic != null && !topic.isBlank()) {
            try {
                Long parsedTopicId = Long.parseLong(topic.trim());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("topicId"), parsedTopicId));
            } catch (NumberFormatException e) {
                spec = spec.and(fieldEquals("topic", topic));
            }
        }

        if (difficulty != null && !difficulty.isBlank()) {
            spec = spec.and(fieldEquals("difficulty", difficulty));
        }
        if (state != null && !state.isBlank()) {
            spec = spec.and(fieldEquals("state", state));
        }
        if (search != null && !search.isBlank()) {
            spec = spec.and(searchLike(search));
        }

        if ((targetLang != null && !targetLang.isBlank()) || (translationStatus != null && !translationStatus.isBlank() && !"ALL".equalsIgnoreCase(translationStatus))) {
            spec = spec.and(buildTranslationSpecification(targetLang, translationStatus, tenantId));
        }

        org.springframework.data.domain.Page<Question> pageResult = questionRepository.findAll(spec, pageable);
        List<UUID> questionIds = pageResult.getContent().stream().map(Question::getId).toList();

        Map<UUID, List<Translation>> translationsByQuestion = Collections.emptyMap();
        if (!questionIds.isEmpty()) {
            List<Translation> translations = translationRepository.findByQuestionIdsAndTenantId(questionIds, tenantId);
            if (translations != null && !translations.isEmpty()) {
                translationsByQuestion = translations.stream().collect(Collectors.groupingBy(Translation::getQuestionId));
            }
        }

        final Map<UUID, List<Translation>> finalTranslationsMap = translationsByQuestion;
        final String effectiveTargetLang = (targetLang != null && !targetLang.isBlank()) ? targetLang.trim().toLowerCase() : null;

        return pageResult.map(q -> toResponse(q, finalTranslationsMap.getOrDefault(q.getId(), Collections.emptyList()), effectiveTargetLang));
    }

    /**
     * Backward-compatible listQuestions methods.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<QuestionResponse> listQuestions(
            String subject, Long subjectId, String topic, Long topicId,
            String difficulty, String state, String search,
            String targetLang, String translationStatus,
            int page, int size, String tenantId) {
        return listQuestions(subject, subjectId, topic, topicId, difficulty, state, search, targetLang, translationStatus, null, "desc", page, size, tenantId);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<QuestionResponse> listQuestions(
            String subject, Long subjectId, String topic, Long topicId,
            String difficulty, String state, String search,
            int page, int size, String tenantId) {
        return listQuestions(subject, subjectId, topic, topicId, difficulty, state, search, null, null, null, "desc", page, size, tenantId);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<QuestionResponse> listQuestions(
            String subject, String topic, String difficulty, String state,
            String search, int page, int size, String tenantId) {
        return listQuestions(subject, null, topic, null, difficulty, state, search, null, null, null, "desc", page, size, tenantId);
    }

    private String resolveQuestionSortProperty(String sort) {
        if (sort == null || sort.isBlank()) return "createdAt";
        return switch (sort.trim().toLowerCase()) {
            case "subject", "subjectname" -> "subject";
            case "topic", "topicname" -> "topic";
            case "subtopic" -> "subtopic";
            case "chapter" -> "chapter";
            case "difficulty" -> "difficulty";
            case "cognitivelevel" -> "cognitiveLevel";
            case "questiontype" -> "questionType";
            case "state", "status" -> "state";
            case "updatedat" -> "updatedAt";
            case "createdat", "created" -> "createdAt";
            case "id" -> "id";
            default -> "createdAt";
        };
    }

    private org.springframework.data.jpa.domain.Specification<Question> tenantEquals(String tenantId) {
        return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }

    private org.springframework.data.jpa.domain.Specification<Question> fieldEquals(String field, String value) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get(field)), value.toLowerCase());
    }

    /**
     * Smart multi-field search specification.
     * Matches across question content, subject, topic, subtopic, chapter, difficulty,
     * cognitiveLevel, questionType, explanation, references, and state.
     * Supports smart token matching where multi-word queries match across different metadata fields.
     */
    public org.springframework.data.jpa.domain.Specification<Question> searchLike(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String cleanSearch = search.trim();
        String fullPattern = "%" + cleanSearch.toLowerCase() + "%";
        String[] tokens = cleanSearch.split("\\s+");

        return (root, query, cb) -> {
            jakarta.persistence.criteria.Predicate fullPhraseMatch = matchAnyField(root, cb, fullPattern);

            if (tokens.length <= 1) {
                return fullPhraseMatch;
            }

            // For multi-token searches (e.g. "Chemistry Reaction", "Physics EASY"),
            // each token must match at least one metadata or content field.
            List<jakarta.persistence.criteria.Predicate> tokenPredicates = new java.util.ArrayList<>();
            for (String token : tokens) {
                if (!token.isBlank()) {
                    String tokenPattern = "%" + token.toLowerCase() + "%";
                    tokenPredicates.add(matchAnyField(root, cb, tokenPattern));
                }
            }

            if (tokenPredicates.isEmpty()) {
                return fullPhraseMatch;
            }

            jakarta.persistence.criteria.Predicate allTokensMatch = cb.and(tokenPredicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            return cb.or(fullPhraseMatch, allTokensMatch);
        };
    }

    private jakarta.persistence.criteria.Predicate matchAnyField(
            jakarta.persistence.criteria.Root<Question> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            String pattern) {
        return cb.or(
                cb.like(cb.lower(cb.coalesce(root.get("subject"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(root.get("topic"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(root.get("subtopic"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(root.get("chapter"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(root.get("difficulty"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(root.get("cognitiveLevel"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(root.get("questionType"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(root.get("content"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(root.get("explanation"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(root.get("state"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(root.get("references"), "")), pattern)
        );
    }

    /**
     * Builds a JPA Specification joining questions with translations for language and status filtering.
     */
    private org.springframework.data.jpa.domain.Specification<Question> buildTranslationSpecification(
            String targetLang, String translationStatus, String tenantId) {
        return (root, query, cb) -> {
            jakarta.persistence.criteria.Subquery<UUID> subquery = query.subquery(UUID.class);
            jakarta.persistence.criteria.Root<Translation> tRoot = subquery.from(Translation.class);
            subquery.select(tRoot.get("questionId"));

            List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            predicates.add(cb.equal(tRoot.get("questionId"), root.get("id")));

            if (tenantId != null && !tenantId.isBlank()) {
                predicates.add(cb.or(
                        cb.equal(tRoot.get("tenantId"), tenantId),
                        cb.equal(tRoot.get("tenantId"), "default")
                ));
            }

            if (targetLang != null && !targetLang.isBlank()) {
                predicates.add(cb.equal(cb.lower(tRoot.get("languageCode")), targetLang.trim().toLowerCase()));
            }

            String status = translationStatus != null ? translationStatus.trim().toUpperCase() : null;
            boolean negate = false;

            if (status != null && !status.isBlank() && !"ALL".equals(status)) {
                switch (status) {
                    case "MISSING":
                    case "UNTRANSLATED":
                        negate = true;
                        break;
                    case "EXISTS":
                        // Existence check already covered by questionId + languageCode
                        break;
                    case "APPROVED":
                        predicates.add(cb.equal(tRoot.get("status"), Translation.TranslationStatus.APPROVED));
                        break;
                    case "PUBLISHED":
                        predicates.add(cb.equal(tRoot.get("status"), Translation.TranslationStatus.PUBLISHED));
                        break;
                    case "APPROVED_PUBLISHED":
                    case "APPROVED_OR_PUBLISHED":
                        predicates.add(tRoot.get("status").in(Translation.TranslationStatus.APPROVED, Translation.TranslationStatus.PUBLISHED));
                        break;
                    case "DRAFT":
                        predicates.add(cb.equal(tRoot.get("status"), Translation.TranslationStatus.DRAFT));
                        break;
                    case "IN_REVIEW":
                    case "PENDING_REVIEW":
                        predicates.add(cb.equal(tRoot.get("status"), Translation.TranslationStatus.DRAFT));
                        break;
                    case "STALE":
                        predicates.add(cb.equal(tRoot.get("status"), Translation.TranslationStatus.STALE));
                        break;
                    case "REJECTED":
                    case "NEEDS_REWORK":
                        jakarta.persistence.criteria.Predicate rejPred = cb.and(
                                cb.equal(tRoot.get("status"), Translation.TranslationStatus.DRAFT),
                                cb.isNotNull(tRoot.get("reviewComments")),
                                cb.notEqual(tRoot.get("reviewComments"), "")
                        );
                        jakarta.persistence.criteria.Predicate stalePred = cb.equal(tRoot.get("status"), Translation.TranslationStatus.STALE);
                        predicates.add(cb.or(rejPred, stalePred));
                        break;
                    default:
                        try {
                            Translation.TranslationStatus enumStatus = Translation.TranslationStatus.valueOf(status);
                            predicates.add(cb.equal(tRoot.get("status"), enumStatus));
                        } catch (IllegalArgumentException ignored) {}
                        break;
                }
            }

            subquery.where(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));

            if (negate) {
                return cb.not(cb.exists(subquery));
            } else {
                return cb.exists(subquery);
            }
        };
    }

    /**
     * Retrieves a question by its ID with translation metadata.
     */
    @Transactional(readOnly = true)
    public QuestionResponse getQuestion(UUID questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("Question not found: " + questionId));
        List<Translation> translations = translationRepository.findByQuestionIdAndTenantId(questionId, question.getTenantId());
        return toResponse(question, translations, null);
    }

    /**
     * Submits a DRAFT question for review — transitions state from DRAFT to REVIEW.
     */
    public QuestionResponse submitForReview(UUID questionId, UUID authorId, String tenantId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("Question not found: " + questionId));

        if (!question.getAuthorId().equals(authorId)) {
            throw new IllegalArgumentException("Only the question author can submit for review");
        }

        if (!"DRAFT".equals(question.getState())) {
            throw new IllegalStateException("Question must be in DRAFT state to submit for review. Current state: " + question.getState());
        }

        question.setState("REVIEW");
        Question saved = questionRepository.save(question);

        log.info("Question submitted for review: id={}, author={}, tenant={}", questionId, authorId, tenantId);

        publishAuditEvent("QUESTION_SUBMITTED_FOR_REVIEW", saved.getId(), authorId, tenantId,
                Map.of("fromState", "DRAFT", "toState", "REVIEW"));

        return toResponse(saved);
    }

    /**
     * Finds approved questions matching blueprint criteria for Paper Generator.
     */
    @Transactional(readOnly = true)
    public List<QuestionResponse> findBlueprintQuestions(String subject, String topic, String difficulty, String cognitiveLevel, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
        List<Question> questions = questionRepository.findBlueprintQuestions(
                subject != null ? subject.trim() : "",
                topic != null ? topic.trim() : "",
                (difficulty != null && !difficulty.isBlank()) ? difficulty.trim() : null,
                (cognitiveLevel != null && !cognitiveLevel.isBlank()) ? cognitiveLevel.trim() : null,
                effectiveTenant
        );

        if (questions.isEmpty()) {
            questions = questionRepository.findBlueprintQuestionsFallback(
                    subject != null ? subject.trim() : "",
                    topic != null ? topic.trim() : "",
                    (difficulty != null && !difficulty.isBlank()) ? difficulty.trim() : null,
                    effectiveTenant
            );
        }

        return questions.stream().map(this::toResponse).toList();
    }

    /**
     * Finds questions by their unique IDs for Paper Generator review.
     */
    @Transactional(readOnly = true)
    public List<QuestionResponse> findQuestionsByIds(List<UUID> questionIds, String tenantId) {
        if (questionIds == null || questionIds.isEmpty()) {
            return List.of();
        }
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
        List<Question> questions = questionRepository.findQuestionsByIdsIn(questionIds, effectiveTenant);
        return questions.stream().map(this::toResponse).toList();
    }

    private void publishAuditEvent(String eventType, UUID questionId, UUID actorId,
                                    String tenantId, Map<String, Object> extra) {
        try {
            Map<String, Object> event = new java.util.HashMap<>();
            event.put("eventType", eventType);
            event.put("questionId", questionId.toString());
            event.put("actorId", actorId.toString());
            event.put("tenantId", tenantId);
            event.put("occurredAt", Instant.now().toString());
            if (extra != null) {
                event.putAll(extra);
            }

            eventPublisher.publish(AUDIT_TOPIC, questionId.toString(), event);
        } catch (Exception e) {
            log.error("Unexpected error publishing audit event [type={}]: {}", eventType, e.getMessage());
        }
    }

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    public QuestionResponse toResponse(Question question) {
        return toResponse(question, Collections.emptyList(), null);
    }

    public QuestionResponse toResponse(Question question, List<Translation> translations, String targetLang) {
        LocalDateTime createdAt = question.getCreatedAt() != null
                ? LocalDateTime.ofInstant(question.getCreatedAt(), ZoneOffset.UTC)
                : null;
        LocalDateTime updatedAt = question.getUpdatedAt() != null
                ? LocalDateTime.ofInstant(question.getUpdatedAt(), ZoneOffset.UTC)
                : null;

        java.util.List<com.examplatform.questionbank.dto.QuestionOption> options = question.getOptions();
        if ((options == null || options.isEmpty())) {
            String questionType = question.getQuestionType();
            if (questionType != null && (questionType.equals("SINGLE_MCQ") || questionType.equals("MULTI_MCQ"))
                    && question.getAnswerKey() != null && question.getAnswerKey().startsWith("[")) {
                try {
                    options = MAPPER.readValue(question.getAnswerKey(),
                            new com.fasterxml.jackson.core.type.TypeReference<
                                    java.util.List<com.examplatform.questionbank.dto.QuestionOption>>() {});
                } catch (Exception ignored) {}
            }
        }

        List<String> translatedLangs = Collections.emptyList();
        Map<String, String> statusMap = Collections.emptyMap();
        String activeTransStatus = null;

        if (translations != null && !translations.isEmpty()) {
            translatedLangs = translations.stream()
                    .map(Translation::getLanguageCode)
                    .filter(l -> l != null && !l.isBlank())
                    .map(String::toLowerCase)
                    .distinct()
                    .toList();

            statusMap = new HashMap<>();
            for (Translation t : translations) {
                if (t.getLanguageCode() != null) {
                    statusMap.put(t.getLanguageCode().toLowerCase(), resolveTranslationStatus(t));
                }
            }

            if (targetLang != null && !targetLang.isBlank()) {
                activeTransStatus = statusMap.get(targetLang.toLowerCase());
                if (activeTransStatus == null) {
                    activeTransStatus = "MISSING";
                }
            }
        } else if (targetLang != null && !targetLang.isBlank()) {
            activeTransStatus = "MISSING";
        }

        return QuestionResponse.builder()
                .id(question.getId())
                .subjectId(question.getSubjectId())
                .topicId(question.getTopicId())
                .subtopicId(question.getSubtopicId())
                .subject(question.getSubject())
                .topic(question.getTopic())
                .subtopic(question.getSubtopic())
                .chapter(question.getChapter())
                .difficulty(question.getDifficulty())
                .cognitiveLevel(question.getCognitiveLevel())
                .questionType(question.getQuestionType())
                .content(question.getContent())
                .answerKey(question.getAnswerKey())
                .explanation(question.getExplanation())
                .references(question.getReferences())
                .state(question.getState())
                .authorId(question.getAuthorId())
                .reviewerId(question.getReviewerId())
                .encryptionKeyId(question.getEncryptionKeyId())
                .passageId(question.getPassageId())
                .passageOrderIndex(question.getPassageOrderIndex())
                .version(question.getVersion())
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .options(options)
                .hasImages(question.isHasImages())
                .translatedLanguages(translatedLangs)
                .translationStatusMap(statusMap)
                .translationStatus(activeTransStatus)
                .build();
    }

    private String resolveTranslationStatus(Translation t) {
        if (t == null) return "MISSING";
        if (t.getStatus() == Translation.TranslationStatus.DRAFT) {
            if (t.getReviewComments() != null && !t.getReviewComments().isBlank()) {
                return "REJECTED";
            }
            return "DRAFT";
        }
        return t.getStatus() != null ? t.getStatus().name() : "DRAFT";
    }

    public static boolean detectHasImages(String content, String explanation, List<com.examplatform.questionbank.dto.QuestionOption> options) {
        if (containsImageTagOrMarkdown(content)) return true;
        if (containsImageTagOrMarkdown(explanation)) return true;
        if (options != null) {
            for (var opt : options) {
                if (opt.getImageUrl() != null && !opt.getImageUrl().isBlank()) return true;
                if (containsImageTagOrMarkdown(opt.getText())) return true;
            }
        }
        return false;
    }

    public static boolean containsImageTagOrMarkdown(String text) {
        if (text == null || text.isBlank()) return false;
        if (text.contains("<img") || text.contains("<svg")) return true;
        if (text.matches(".*!\\[[^\\]]*\\]\\([^)]+\\).*")) return true;
        return false;
    }
}
