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

package com.examplatform.questionbank.service;

import com.examplatform.questionbank.ai.embedding.EmbeddingService;
import com.examplatform.questionbank.ai.similarity.SimilarityCheckResult;
import com.examplatform.questionbank.domain.*;
import com.examplatform.questionbank.domain.enums.DifficultyLevel;
import com.examplatform.questionbank.domain.enums.QuestionType;
import com.examplatform.questionbank.dto.CreateQuestionRequest;
import com.examplatform.questionbank.dto.QuestionResponse;
import com.examplatform.questionbank.exception.SimilarQuestionException;
import com.examplatform.questionbank.repository.*;
import com.examplatform.questionbank.translation.domain.Translation;
import com.examplatform.questionbank.translation.repository.TranslationRepository;
import com.examplatform.questionbank.util.EmbeddingUtils;
import com.examplatform.shared.constants.QuestionStateConstants;
import com.examplatform.shared.constants.QuestionTypeConstants;
import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.messaging.EventPublisher;
import com.examplatform.shared.taxonomy.TaxonomyClient;
import com.examplatform.shared.taxonomy.TaxonomyNames;
import com.examplatform.shared.taxonomy.TaxonomyNodeRef;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;
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
public class QuestionService {

    private static final String AUDIT_EVENTS_TOPIC = "audit-events";
    private static final String FIELD_QUESTION_TYPE = "questionType";
    private static final String FIELD_STATE = "state";
    private static final String FIELD_CHAPTER = "chapter";
    private static final String FIELD_SUBJECT_ID = "subjectId";
    private static final String FIELD_TOPIC_ID = "topicId";
    private static final String FIELD_SUBTOPIC_ID = "subtopicId";
    private static final String FIELD_UPDATED_AT = "updatedAt";
    private static final String STATE_REJECTED = "REJECTED";
    private static final String STATUS_MISSING = "MISSING";
    private static final String QUESTION_NOT_FOUND_PREFIX = "Question not found: ";
    private static final Pattern MARKDOWN_IMAGE_PATTERN = Pattern.compile("!\\[[^\\]]*\\]\\([^)]+\\)");

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    private static final List<String> SEARCHABLE_QUESTION_FIELDS = List.of(
            FIELD_CHAPTER, SharedConstants.DIFFICULTY,
            "cognitiveLevel", FIELD_QUESTION_TYPE, SharedConstants.CONTENT, "explanation", FIELD_STATE, "sourceReferences"
    );

    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final SubtopicRepository subtopicRepository;
    private final SimilarityDetectionService similarityDetectionService;
    private final EmbeddingService embeddingService;
    private final EventPublisher eventPublisher;
    private final TranslationRepository translationRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final TaxonomyClient taxonomyClient;
    private final QuestionService self;

    @Value("${app.encryption.enabled:true}")
    private boolean encryptionEnabled;

    @SuppressWarnings("java:S107")
    @Autowired
    public QuestionService(
            QuestionRepository questionRepository,
            SubjectRepository subjectRepository,
            TopicRepository topicRepository,
            SubtopicRepository subtopicRepository,
            SimilarityDetectionService similarityDetectionService,
            EmbeddingService embeddingService,
            EventPublisher eventPublisher,
            TranslationRepository translationRepository,
            QuestionVersionRepository questionVersionRepository,
            TaxonomyClient taxonomyClient,
            @Lazy QuestionService self) {
        this.questionRepository = questionRepository;
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.subtopicRepository = subtopicRepository;
        this.similarityDetectionService = similarityDetectionService;
        this.embeddingService = embeddingService;
        this.eventPublisher = eventPublisher;
        this.translationRepository = translationRepository;
        this.questionVersionRepository = questionVersionRepository;
        this.taxonomyClient = taxonomyClient;
        this.self = self != null ? self : this;
    }

    @SuppressWarnings("java:S107")
    public QuestionService(
            QuestionRepository questionRepository,
            SubjectRepository subjectRepository,
            TopicRepository topicRepository,
            SubtopicRepository subtopicRepository,
            SimilarityDetectionService similarityDetectionService,
            EmbeddingService embeddingService,
            EventPublisher eventPublisher,
            TranslationRepository translationRepository,
            QuestionVersionRepository questionVersionRepository,
            TaxonomyClient taxonomyClient) {
        this(questionRepository, subjectRepository, topicRepository, subtopicRepository,
                similarityDetectionService, embeddingService, eventPublisher,
                translationRepository, questionVersionRepository, taxonomyClient, null);
    }

    /**
     * Resolves and validates the numeric Subject -> Topic -> Subtopic hierarchy.
     */
    public record ResolvedHierarchy(
            Long subjectId, String subjectName,
            Long topicId, String topicName,
            Long subtopicId, String subtopicName
    ) {}

    /**
     * Filter criteria for listing questions with paging and sorting.
     */
    public record QuestionFilterCriteria(
            String subject, Long subjectId, String topic, Long topicId, Long subtopicId,
            String difficulty, String state, String search,
            String targetLang, String translationStatus,
            String sort, String order,
            int page, int size, String tenantId
    ) {}

    public ResolvedHierarchy resolveHierarchy(CreateQuestionRequest request, String tenantId) {
        if (request.getSubjectId() == null) {
            throw new IllegalArgumentException("subjectId is required to create a question");
        }
        if (request.getTopicId() == null) {
            throw new IllegalArgumentException("topicId is required to create a question");
        }
        if (request.getSubtopicId() == null) {
            throw new IllegalArgumentException("subtopicId is required to create a question");
        }

        if (taxonomyClient != null && !taxonomyClient.validateChain(request.getSubjectId(), request.getTopicId(), request.getSubtopicId(), tenantId)) {
            throw new IllegalArgumentException("Invalid taxonomy hierarchy chain: subjectId=" + request.getSubjectId()
                    + ", topicId=" + request.getTopicId() + ", subtopicId=" + request.getSubtopicId() + " in tenant " + tenantId);
        }

        TaxonomyNames names = taxonomyClient != null
                ? taxonomyClient.resolveNames(new TaxonomyNodeRef(request.getSubjectId(), request.getTopicId(), request.getSubtopicId()), tenantId)
                : new TaxonomyNames(null, null, null);

        String sName = names.subjectName() != null ? names.subjectName()
                : subjectRepository.findById(request.getSubjectId()).map(Subject::getName).orElse("Unknown Subject");
        String tName = names.topicName() != null ? names.topicName()
                : topicRepository.findById(request.getTopicId()).map(Topic::getName).orElse("Unknown Topic");
        String stName = names.subtopicName() != null ? names.subtopicName()
                : subtopicRepository.findById(request.getSubtopicId()).map(Subtopic::getName).orElse("Unknown Subtopic");

        return new ResolvedHierarchy(
                request.getSubjectId(), sName,
                request.getTopicId(), tName,
                request.getSubtopicId(), stName);
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
        if (request.getQuestionType() == null) {
            throw new IllegalArgumentException("Question type must be one of the supported types: "
                    + "SINGLE_MCQ, MULTI_MCQ, NUMERICAL, DESCRIPTIVE, MATRIX_MATCH, ASSERTION_REASON, CODING, CASE_STUDY");
        }

        ResolvedHierarchy hierarchy = resolveHierarchy(request, tenantId);
        SimilarityCheckResult similarityResult = checkSimilarity(request.getContent(), hierarchy.subjectName(), tenantId);

        String dekKeyName = encryptionEnabled ? "question-dek-" + UUID.randomUUID() : null;
        String answerKey = validateAndSerializeOptions(request);
        boolean hasImages = detectHasImages(request.getContent(), request.getExplanation(), request.getOptions());

        Question question = buildQuestionEntity(request, hierarchy, authorId, tenantId, dekKeyName, answerKey, hasImages);
        Question saved = questionRepository.save(question);

        generateAndStoreEmbedding(saved, request.getContent());

        log.info("Question created: id={}, type={}, author={}, tenant={}, encrypted={}",
                saved.getId(), saved.getQuestionType(), authorId, tenantId, encryptionEnabled);

        publishAuditEvent("QUESTION_CREATED", saved.getId(), authorId, tenantId,
                Map.of(FIELD_QUESTION_TYPE, saved.getQuestionType(), FIELD_STATE, saved.getState()));

        QuestionResponse response = toResponse(saved);
        attachSimilarWarnings(response, similarityResult);

        return response;
    }

    private SimilarityCheckResult checkSimilarity(String content, String subjectName, String tenantId) {
        try {
            return similarityDetectionService.enforceNoDuplicate(content, subjectName, tenantId);
        } catch (SimilarQuestionException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Similarity check unavailable during question creation. " +
                    "Proceeding without duplicate detection. Reason: {}", e.getMessage());
            return null;
        }
    }

    private String validateAndSerializeOptions(CreateQuestionRequest request) {
        String answerKey = request.getAnswerKey();
        var options = request.getOptions();
        if (options == null || options.isEmpty()) {
            return answerKey;
        }
        if (options.size() < 2 || options.size() > 5) {
            throw new IllegalArgumentException("MCQ/MSQ questions must have between 2 and 5 options");
        }
        String[] ids = {"A", "B", "C", "D", "E", "F"};
        for (int i = 0; i < options.size(); i++) {
            options.get(i).setId(ids[i]);
        }
        long correctCount = options.stream().filter(com.examplatform.questionbank.dto.QuestionOption::isCorrect).count();
        QuestionType questionType = request.getQuestionType();
        if (questionType == QuestionType.SINGLE_MCQ && correctCount != 1) {
            throw new IllegalArgumentException("MCQ questions must have exactly one correct option");
        } else if (questionType == QuestionType.MULTI_MCQ && correctCount < 1) {
            throw new IllegalArgumentException("MSQ questions must have at least one correct option");
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(options);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize options", e);
        }
    }

    private Question buildQuestionEntity(
            CreateQuestionRequest request,
            ResolvedHierarchy hierarchy,
            UUID authorId,
            String tenantId,
            String dekKeyName,
            String answerKey,
            boolean hasImages) {
        Question question = Question.builder()
                .subjectId(hierarchy.subjectId())
                .topicId(hierarchy.topicId())
                .subtopicId(hierarchy.subtopicId())
                .chapter(request.getChapter())
                .difficulty(request.getDifficulty().name())
                .benchmarkTimeSeconds(resolveBenchmarkTimeSeconds(request.getBenchmarkTimeSeconds(), request.getDifficulty()))
                .cognitiveLevel(request.getCognitiveLevel().name())
                .questionType(request.getQuestionType().name())
                .content(request.getContent())
                .answerKey(answerKey)
                .options(request.getOptions())
                .explanation(request.getExplanation())
                .sourceReferences(request.getReferences())
                .hasImages(hasImages)
                .passageId(request.getPassageId())
                .passageOrderIndex(request.getPassageOrderIndex())
                .state((request.getState() != null && !request.getState().isBlank()) ? request.getState() :
                        QuestionStateConstants.DRAFT)
                .encryptionKeyId(dekKeyName)
                .authorId(authorId)
                .build();

        question.setTenantId(tenantId);
        return question;
    }

    private void generateAndStoreEmbedding(Question saved, String content) {
        try {
            float[] embedding = embeddingService.embed(content);
            if (embedding != null && embedding.length > 0) {
                questionRepository.updateEmbedding(saved.getId(), EmbeddingUtils.embeddingToString(embedding));
                log.debug("Embedding generated and stored for question: id={}", saved.getId());
            }
        } catch (Exception e) {
            log.warn("Failed to generate embedding for question id={}. " +
                    "Question created without embedding. Reason: {}", saved.getId(), e.getMessage());
        }
    }

    private void attachSimilarWarnings(QuestionResponse response, SimilarityCheckResult similarityResult) {
        if (similarityResult != null && similarityResult.status() == SimilarityCheckResult.Status.WARN) {
            List<QuestionResponse.SimilarQuestionWarning> warnings = similarityResult.similarQuestions().stream()
                    .map(sq -> QuestionResponse.SimilarQuestionWarning.builder()
                            .questionId(sq.questionId())
                            .similarity(sq.similarity())
                            .contentSnippet(sq.content())
                            .build())
                    .toList();
            response.setWarnings(warnings);
        }
    }

    /**
     * Persists an AI-generated question conforming to system hierarchy,
     * duplicate detection, envelope encryption, and audit tracking.
     */
    public QuestionResponse createQuestionFromGenerated(
            CreateQuestionRequest request,
            UUID authorId,
            String tenantId) {
        if (request.getState() == null || request.getState().isBlank()) {
            request.setState(QuestionStateConstants.DRAFT);
        }
        return createQuestion(request, authorId, tenantId);
    }

    /**
     * Persists a batch of AI-generated questions as DRAFT items.
     */
    public List<QuestionResponse> createQuestionsFromGeneratedBatch(
            List<CreateQuestionRequest> requests,
            UUID authorId,
            String tenantId) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        return requests.stream()
                .map(r -> createQuestionFromGenerated(r, authorId, tenantId))
                .toList();
    }

    /**
     * Retrieves aggregated metrics for Question Bank items.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getQuestionBankMetrics(String tenantId) {
        long total = questionRepository.countByTenantId(tenantId);
        long draft = questionRepository.countByStateAndTenantId(QuestionStateConstants.DRAFT, tenantId);
        long submitted = questionRepository.countByStateAndTenantId("SUBMITTED", tenantId)
                + questionRepository.countByStateAndTenantId("UNDER_REVIEW", tenantId);
        long approved = questionRepository.countByStateAndTenantId(QuestionStateConstants.APPROVED, tenantId)
                + questionRepository.countByStateAndTenantId(QuestionStateConstants.PUBLISHED, tenantId);
        long rejected = questionRepository.countByStateAndTenantId(STATE_REJECTED, tenantId)
                + questionRepository.countByStateAndTenantId("REVISION_REQUESTED", tenantId);

        return Map.of(
                "total", total,
                "draft", draft,
                "submitted", submitted,
                "approved", approved,
                "rejected", rejected
        );
    }

    /**
     * Lists questions for a tenant with optional filtering by hierarchy, difficulty, state, text search,
     * target language, translation status, and dynamic sorting.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<QuestionResponse> listQuestions(QuestionFilterCriteria criteria) {
        org.springframework.data.domain.Sort sortSpec = resolveSortSpec(criteria.sort(), criteria.order());
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(criteria.page(), criteria.size(), sortSpec);

        org.springframework.data.jpa.domain.Specification<Question> spec = buildQuestionListSpecification(criteria);
        org.springframework.data.domain.Page<Question> pageResult = questionRepository.findAll(spec, pageable);

        List<UUID> questionIds = pageResult.getContent().stream().map(Question::getId).toList();
        Map<UUID, List<Translation>> translationsByQuestion = loadTranslationsByQuestion(questionIds, criteria.tenantId());
        String effectiveTargetLang = (criteria.targetLang() != null && !criteria.targetLang().isBlank())
                ? criteria.targetLang().trim().toLowerCase() : null;

        return pageResult.map(q -> toResponse(q, translationsByQuestion.getOrDefault(q.getId(), Collections.emptyList()), effectiveTargetLang));
    }

    @SuppressWarnings("java:S107")
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<QuestionResponse> listQuestions(
            String subject, Long subjectId, String topic, Long topicId, Long subtopicId,
            String difficulty, String state, String search,
            String targetLang, String translationStatus,
            String sort, String order,
            int page, int size, String tenantId) {
        return self.listQuestions(new QuestionFilterCriteria(
                subject, subjectId, topic, topicId, subtopicId,
                difficulty, state, search,
                targetLang, translationStatus,
                sort, order,
                page, size, tenantId));
    }

    /**
     * Backward-compatible listQuestions methods.
     */
    @SuppressWarnings("java:S107")
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<QuestionResponse> listQuestions(
            String subject, Long subjectId, String topic, Long topicId,
            String difficulty, String state, String search,
            String targetLang, String translationStatus,
            String sort, String order,
            int page, int size, String tenantId) {
        return self.listQuestions(subject, subjectId, topic, topicId, null, difficulty, state, search, targetLang, translationStatus, sort, order, page, size, tenantId);
    }

    @SuppressWarnings("java:S107")
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<QuestionResponse> listQuestions(
            String subject, Long subjectId, String topic, Long topicId,
            String difficulty, String state, String search,
            String targetLang, String translationStatus,
            int page, int size, String tenantId) {
        return self.listQuestions(subject, subjectId, topic, topicId, null, difficulty, state, search, targetLang, translationStatus, FIELD_UPDATED_AT, "desc", page, size, tenantId);
    }

    @SuppressWarnings("java:S107")
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<QuestionResponse> listQuestions(
            String subject, Long subjectId, String topic, Long topicId,
            String difficulty, String state, String search,
            int page, int size, String tenantId) {
        return self.listQuestions(subject, subjectId, topic, topicId, null, difficulty, state, search, null, null, FIELD_UPDATED_AT, "desc", page, size, tenantId);
    }

    @SuppressWarnings("java:S107")
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<QuestionResponse> listQuestions(
            String subject, String topic, String difficulty, String state,
            String search, int page, int size, String tenantId) {
        return self.listQuestions(subject, null, topic, null, null, difficulty, state, search, null, null, FIELD_UPDATED_AT, "desc", page, size, tenantId);
    }

    private org.springframework.data.jpa.domain.Specification<Question> buildQuestionListSpecification(
            QuestionFilterCriteria criteria) {
        org.springframework.data.jpa.domain.Specification<Question> spec =
                org.springframework.data.jpa.domain.Specification.where(tenantEquals(criteria.tenantId()));

        org.springframework.data.jpa.domain.Specification<Question> subjectSpec =
                buildSubjectSpecification(criteria.subjectId(), criteria.subject(), criteria.tenantId());
        if (subjectSpec != null) {
            spec = spec.and(subjectSpec);
        }

        org.springframework.data.jpa.domain.Specification<Question> topicSpec =
                buildTopicSpecification(criteria.topicId(), criteria.topic(), criteria.subjectId(), criteria.tenantId());
        if (topicSpec != null) {
            spec = spec.and(topicSpec);
        }

        if (criteria.subtopicId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get(FIELD_SUBTOPIC_ID), criteria.subtopicId()));
        }
        if (criteria.difficulty() != null && !criteria.difficulty().isBlank()) {
            spec = spec.and(fieldEquals(SharedConstants.DIFFICULTY, criteria.difficulty()));
        }
        if (criteria.state() != null && !criteria.state().isBlank()) {
            spec = spec.and(fieldEquals(FIELD_STATE, criteria.state()));
        }
        if (criteria.search() != null && !criteria.search().isBlank()) {
            spec = spec.and(searchLike(criteria.search()));
        }

        org.springframework.data.jpa.domain.Specification<Question> transSpec =
                buildTranslationSpecification(criteria.targetLang(), criteria.translationStatus(), criteria.tenantId());
        if (transSpec != null) {
            spec = spec.and(transSpec);
        }

        return spec;
    }

    private org.springframework.data.domain.Sort resolveSortSpec(String sort, String order) {
        org.springframework.data.domain.Sort.Direction direction = "asc".equalsIgnoreCase(order)
                ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;

        if (sort == null || sort.isBlank() || FIELD_UPDATED_AT.equalsIgnoreCase(sort.trim())) {
            return org.springframework.data.domain.Sort.by(direction, FIELD_UPDATED_AT)
                    .and(org.springframework.data.domain.Sort.by(direction, SharedConstants.CREATED_AT));
        }

        String sortProperty = resolveQuestionSortProperty(sort);
        return org.springframework.data.domain.Sort.by(direction, sortProperty)
                .and(Sort.by(Sort.Direction.DESC, SharedConstants.CREATED_AT));
    }

    private org.springframework.data.jpa.domain.Specification<Question> buildSubjectSpecification(
            Long subjectId, String subject, String tenantId) {
        if (subjectId != null) {
            return (root, query, cb) -> cb.equal(root.get(FIELD_SUBJECT_ID), subjectId);
        }
        if (subject != null && !subject.isBlank()) {
            try {
                Long parsedId = Long.parseLong(subject.trim());
                return (root, query, cb) -> cb.equal(root.get(FIELD_SUBJECT_ID), parsedId);
            } catch (NumberFormatException _) {
                Long resolvedSubjId = subjectRepository.findByNameAndTenantId(subject.trim(), tenantId)
                        .or(() -> subjectRepository.findByNameAndTenantIdIsNull(subject.trim()))
                        .map(Subject::getId).orElse(null);
                if (resolvedSubjId != null) {
                    return (root, query, cb) -> cb.equal(root.get(FIELD_SUBJECT_ID), resolvedSubjId);
                }
            }
        }
        return null;
    }

    private org.springframework.data.jpa.domain.Specification<Question> buildTopicSpecification(
            Long topicId, String topic, Long subjectId, String tenantId) {
        if (topicId != null) {
            return (root, query, cb) -> cb.equal(root.get(FIELD_TOPIC_ID), topicId);
        }
        if (topic != null && !topic.isBlank()) {
            try {
                Long parsedTopicId = Long.parseLong(topic.trim());
                return (root, query, cb) -> cb.equal(root.get(FIELD_TOPIC_ID), parsedTopicId);
            } catch (NumberFormatException _) {
                Long resolvedTopicId = topicRepository.findByNameAndSubjectIdAndTenantId(topic.trim(), subjectId, tenantId)
                        .or(() -> topicRepository.findByNameAndSubjectIdAndTenantIdIsNull(topic.trim(), subjectId))
                        .map(Topic::getId).orElse(null);
                if (resolvedTopicId != null) {
                    return (root, query, cb) -> cb.equal(root.get(FIELD_TOPIC_ID), resolvedTopicId);
                }
            }
        }
        return null;
    }

    private Map<UUID, List<Translation>> loadTranslationsByQuestion(List<UUID> questionIds, String tenantId) {
        if (questionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Translation> translations = translationRepository.findByQuestionIdsAndTenantId(questionIds, tenantId);
        if (translations == null || translations.isEmpty()) {
            return Collections.emptyMap();
        }
        return translations.stream().collect(Collectors.groupingBy(Translation::getQuestionId));
    }

    private String resolveQuestionSortProperty(String sort) {
        if (sort == null || sort.isBlank()) return FIELD_UPDATED_AT;
        return switch (sort.trim().toLowerCase()) {
            case SharedConstants.SUBJECT, "subjectname", "subjectid" -> FIELD_SUBJECT_ID;
            case "topic", "topicname", "topicid" -> FIELD_TOPIC_ID;
            case "subtopic", "subtopicname", "subtopicid" -> FIELD_SUBTOPIC_ID;
            case FIELD_CHAPTER -> FIELD_CHAPTER;
            case SharedConstants.DIFFICULTY -> SharedConstants.DIFFICULTY;
            case "cognitivelevel" -> "cognitiveLevel";
            case "questiontype" -> FIELD_QUESTION_TYPE;
            case FIELD_STATE, SharedConstants.STATUS -> FIELD_STATE;
            case "createdat", "created" -> SharedConstants.CREATED_AT;
            case "id" -> "id";
            default -> FIELD_UPDATED_AT;
        };
    }

    private org.springframework.data.jpa.domain.Specification<Question> tenantEquals(String tenantId) {
        return (root, query, cb) -> cb.equal(root.get(SharedConstants.TENANT_ID), tenantId);
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
        jakarta.persistence.criteria.Predicate[] predicates = SEARCHABLE_QUESTION_FIELDS.stream()
                .map(field -> cb.like(cb.lower(cb.coalesce(root.get(field), "")), pattern))
                .toArray(jakarta.persistence.criteria.Predicate[]::new);
        return cb.or(predicates);
    }

    /**
     * Builds a JPA Specification joining questions with translations for language and status filtering.
     */
    private org.springframework.data.jpa.domain.Specification<Question> buildTranslationSpecification(
            String targetLang, String translationStatus, String tenantId) {
        if (translationStatus == null || translationStatus.isBlank() || "ALL".equalsIgnoreCase(translationStatus)) {
            return null;
        }

        String normalizedTargetLang = targetLang != null ? targetLang.trim().toLowerCase() : null;
        String status = translationStatus.trim().toUpperCase();

        // English is the source language for all platform questions
        if ("en".equals(normalizedTargetLang)) {
            if (isNegatedStatus(status)) {
                return (root, query, cb) -> cb.disjunction(); // No questions are missing English
            }
            return null;
        }

        return (root, query, cb) -> {
            jakarta.persistence.criteria.Subquery<UUID> subquery = query.subquery(UUID.class);
            jakarta.persistence.criteria.Root<Translation> tRoot = subquery.from(Translation.class);
            subquery.select(tRoot.get(SharedConstants.QUESTION_ID));

            List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            predicates.add(cb.equal(tRoot.get(SharedConstants.QUESTION_ID), root.get("id")));

            if (tenantId != null && !tenantId.isBlank()) {
                predicates.add(cb.or(
                        cb.equal(tRoot.get(SharedConstants.TENANT_ID), tenantId),
                        cb.equal(tRoot.get(SharedConstants.TENANT_ID), SharedConstants.DEFAULT_TENANT)
                ));
            }

            if (normalizedTargetLang != null && !normalizedTargetLang.isBlank()) {
                predicates.add(cb.equal(cb.lower(tRoot.get("languageCode")), normalizedTargetLang));
            }

            boolean negate = isNegatedStatus(status);
            addStatusPredicate(predicates, cb, tRoot, status);

            subquery.where(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));

            return negate ? cb.not(cb.exists(subquery)) : cb.exists(subquery);
        };
    }

    private static boolean isNegatedStatus(String status) {
        return STATUS_MISSING.equals(status) || "UNTRANSLATED".equals(status);
    }

    private void addStatusPredicate(
            List<jakarta.persistence.criteria.Predicate> predicates,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            jakarta.persistence.criteria.Root<Translation> tRoot,
            String status) {
        switch (status) {
            case STATUS_MISSING, "UNTRANSLATED", "EXISTS":
                // Existence check already covered by questionId + languageCode (negated or positive)
                break;
            case QuestionStateConstants.APPROVED:
                predicates.add(cb.equal(tRoot.get(SharedConstants.STATUS), Translation.TranslationStatus.APPROVED));
                break;
            case QuestionStateConstants.PUBLISHED:
                predicates.add(cb.equal(tRoot.get(SharedConstants.STATUS), Translation.TranslationStatus.PUBLISHED));
                break;
            case "APPROVED_PUBLISHED", "APPROVED_OR_PUBLISHED":
                predicates.add(tRoot.get(SharedConstants.STATUS).in(Translation.TranslationStatus.APPROVED, Translation.TranslationStatus.PUBLISHED));
                break;
            case QuestionStateConstants.DRAFT, "IN_REVIEW", "PENDING_REVIEW":
                predicates.add(cb.equal(tRoot.get(SharedConstants.STATUS), Translation.TranslationStatus.DRAFT));
                break;
            case "STALE":
                predicates.add(cb.equal(tRoot.get(SharedConstants.STATUS), Translation.TranslationStatus.STALE));
                break;
            case STATE_REJECTED, "NEEDS_REWORK":
                jakarta.persistence.criteria.Predicate rejPred = cb.and(
                        cb.equal(tRoot.get(SharedConstants.STATUS), Translation.TranslationStatus.DRAFT),
                        cb.isNotNull(tRoot.get("reviewComments")),
                        cb.notEqual(tRoot.get("reviewComments"), "")
                );
                jakarta.persistence.criteria.Predicate stalePred = cb.equal(tRoot.get(SharedConstants.STATUS), Translation.TranslationStatus.STALE);
                predicates.add(cb.or(rejPred, stalePred));
                break;
            default:
                try {
                    Translation.TranslationStatus enumStatus = Translation.TranslationStatus.valueOf(status);
                    predicates.add(cb.equal(tRoot.get(SharedConstants.STATUS), enumStatus));
                } catch (IllegalArgumentException e) {
                    log.debug("Unknown or non-standard translation status string '{}': {}", status, e.getMessage());
                }
                break;
        }
    }

    /**
     * Retrieves a question by its ID with translation metadata.
     */
    @Transactional(readOnly = true)
    public QuestionResponse getQuestion(UUID questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException(QUESTION_NOT_FOUND_PREFIX + questionId));
        List<Translation> translations = translationRepository.findByQuestionIdAndTenantId(questionId, question.getTenantId());
        return toResponse(question, translations, null);
    }

    /**
     * Submits a DRAFT question for review — transitions state from DRAFT to REVIEW.
     */
    public QuestionResponse submitForReview(UUID questionId, UUID authorId, String tenantId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException(QUESTION_NOT_FOUND_PREFIX + questionId));

        if (!question.getAuthorId().equals(authorId)) {
            throw new IllegalArgumentException("Only the question author can submit for review");
        }

        if (!QuestionStateConstants.DRAFT.equals(question.getState())) {
            throw new IllegalStateException("Question must be in DRAFT state to submit for review. Current state: " + question.getState());
        }

        question.setState(QuestionStateConstants.REVIEW);
        Question saved = questionRepository.save(question);

        log.info("Question submitted for review: id={}, author={}, tenant={}", questionId, authorId, tenantId);

        publishAuditEvent("QUESTION_SUBMITTED_FOR_REVIEW", saved.getId(), authorId, tenantId,
                Map.of("fromState", QuestionStateConstants.DRAFT, "toState", QuestionStateConstants.REVIEW));

        return toResponse(saved);
    }

    /**
     * Deletes a question and cleans up associated versions and translations.
     * Rejects deletion if question is in PUBLISHED state or has been used in exams.
     *
     * @param questionId UUID of the question to delete
     * @param actorId    UUID of the user requesting deletion
     * @param tenantId   optional tenant identifier to verify tenant scoping
     */
    public void deleteQuestion(UUID questionId, UUID actorId, String tenantId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException(QUESTION_NOT_FOUND_PREFIX + questionId));

        if (tenantId != null && !tenantId.isBlank() && !SharedConstants.DEFAULT_TENANT.equals(tenantId)
                && !tenantId.equals(question.getTenantId())) {
            throw new EntityNotFoundException(QUESTION_NOT_FOUND_PREFIX + questionId);
        }

        if (QuestionStateConstants.PUBLISHED.equalsIgnoreCase(question.getState()) || question.getUsageCount() > 0) {
            throw new IllegalStateException("Cannot delete question in PUBLISHED state or with active exam usage. Transition to ARCHIVED instead.");
        }

        // Clean up translations
        List<Translation> translations = translationRepository.findByQuestionIdAndTenantId(questionId, question.getTenantId());
        if (translations != null && !translations.isEmpty()) {
            translationRepository.deleteAll(translations);
        }

        // Clean up version history
        List<QuestionVersion> versions = questionVersionRepository.findByQuestionIdOrderByVersionNumberDesc(questionId);
        if (versions != null && !versions.isEmpty()) {
            questionVersionRepository.deleteAll(versions);
        }

        questionRepository.delete(question);

        log.info("Deleted question id={} by actor={} tenant={}", questionId, actorId, question.getTenantId());

        Map<String, Object> extra = new HashMap<>();
        if (question.getQuestionType() != null) {
            extra.put(FIELD_QUESTION_TYPE, question.getQuestionType());
        }
        if (question.getState() != null) {
            extra.put(FIELD_STATE, question.getState());
        }
        publishAuditEvent("QUESTION_DELETED", questionId, actorId != null ? actorId : question.getAuthorId(),
                question.getTenantId(), extra);
    }

    public void deleteQuestion(UUID questionId, String tenantId) {
        deleteQuestion(questionId, null, tenantId);
    }

    /**
     * Finds published questions matching blueprint criteria by numeric IDs.
     */
    @Transactional(readOnly = true)
    public List<QuestionResponse> findBlueprintQuestions(Long subjectId, Long topicId, Long subtopicId, String difficulty, String cognitiveLevel, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : SharedConstants.DEFAULT_TENANT;
        List<Question> questions = questionRepository.findBlueprintQuestions(
                subjectId,
                topicId,
                subtopicId,
                (difficulty != null && !difficulty.isBlank()) ? difficulty.trim() : null,
                (cognitiveLevel != null && !cognitiveLevel.isBlank()) ? cognitiveLevel.trim() : null,
                effectiveTenant
        );

        if (questions.isEmpty()) {
            questions = questionRepository.findBlueprintQuestionsFallback(
                    subjectId,
                    topicId,
                    subtopicId,
                    (difficulty != null && !difficulty.isBlank()) ? difficulty.trim() : null,
                    effectiveTenant
            );
        }

        return questions.stream().map(this::toResponse).toList();
    }

    /**
     * Finds published questions matching blueprint criteria for Paper Generator (legacy string overload).
     */
    @Transactional(readOnly = true)
    public List<QuestionResponse> findBlueprintQuestions(String subject, String topic, String difficulty, String cognitiveLevel, String tenantId) {
        Long subjId = null;
        if (subject != null && !subject.isBlank()) {
            try {
                subjId = Long.parseLong(subject.trim());
            } catch (NumberFormatException _) {
                subjId = subjectRepository.findByNameAndTenantId(subject.trim(), tenantId)
                        .or(() -> subjectRepository.findByNameAndTenantIdIsNull(subject.trim()))
                        .map(Subject::getId).orElse(null);
            }
        }
        Long topId = null;
        if (topic != null && !topic.isBlank()) {
            try {
                topId = Long.parseLong(topic.trim());
            } catch (NumberFormatException _) {
                Long finalSubjId = subjId;
                topId = topicRepository.findByNameAndSubjectIdAndTenantId(topic.trim(), finalSubjId, tenantId)
                        .or(() -> topicRepository.findByNameAndSubjectIdAndTenantIdIsNull(topic.trim(), finalSubjId))
                        .map(Topic::getId).orElse(null);
            }
        }
        return self.findBlueprintQuestions(subjId, topId, null, difficulty, cognitiveLevel, tenantId);
    }

    /**
     * Finds questions by their unique IDs for Paper Generator review.
     */
    @Transactional(readOnly = true)
    public List<QuestionResponse> findQuestionsByIds(List<UUID> questionIds, String tenantId) {
        if (questionIds == null || questionIds.isEmpty()) {
            return List.of();
        }
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : SharedConstants.DEFAULT_TENANT;
        List<Question> questions = questionRepository.findQuestionsByIdsIn(questionIds, effectiveTenant);
        return questions.stream().map(this::toResponse).toList();
    }

    private void publishAuditEvent(String eventType, UUID questionId, UUID actorId,
                                    String tenantId, Map<String, Object> extra) {
        AuditEventHelper.publishAuditEvent(eventPublisher, log, new AuditEventHelper.AuditEventParams(AUDIT_EVENTS_TOPIC, eventType,
            SharedConstants.QUESTION_ID, questionId, actorId, tenantId, extra));
    }

    public QuestionResponse toResponse(Question question) {
        return toResponse(question, Collections.emptyList(), null);
    }

    public QuestionResponse toResponse(Question question, List<Translation> translations, String targetLang) {
        List<com.examplatform.questionbank.dto.QuestionOption> options = resolveOptions(question);
        TranslationMetadata transMeta = resolveTranslationMetadata(question, translations, targetLang);

        TaxonomyNames names = taxonomyClient != null
                ? taxonomyClient.resolveNames(new TaxonomyNodeRef(question.getSubjectId(), question.getTopicId(), question.getSubtopicId()), question.getTenantId())
                : new TaxonomyNames(null, null, null);

        return QuestionResponse.builderFrom(question)
                .subject(names.subjectName())
                .topic(names.topicName())
                .subtopic(names.subtopicName())
                .options(options)
                .translatedLanguages(transMeta.translatedLangs())
                .translationStatusMap(transMeta.statusMap())
                .translationStatus(transMeta.activeTransStatus())
                .build();
    }

    private List<com.examplatform.questionbank.dto.QuestionOption> resolveOptions(Question question) {
        List<com.examplatform.questionbank.dto.QuestionOption> options = question.getOptions();
        if (options != null && !options.isEmpty()) {
            return options;
        }
        String questionType = question.getQuestionType();
        boolean isMcq = questionType != null && (questionType.equals(QuestionTypeConstants.SINGLE_MCQ)
                || questionType.equals(QuestionTypeConstants.MULTI_MCQ));
        if (isMcq && question.getAnswerKey() != null && question.getAnswerKey().startsWith("[")) {
            try {
                return MAPPER.readValue(question.getAnswerKey(), new com.fasterxml.jackson.core.type.TypeReference<>() {});
            } catch (Exception e) {
                log.debug("Malformed JSON options in answerKey: {}", e.getMessage());
            }
        }
        return options;
    }

    private record TranslationMetadata(
            List<String> translatedLangs,
            Map<String, String> statusMap,
            String activeTransStatus
    ) {}

    private TranslationMetadata resolveTranslationMetadata(Question question, List<Translation> translations, String targetLang) {
        if (translations == null || translations.isEmpty()) {
            return new TranslationMetadata(
                    Collections.emptyList(),
                    Collections.emptyMap(),
                    resolveActiveTransStatus(question, Collections.emptyMap(), targetLang)
            );
        }

        List<String> translatedLangs = translations.stream()
                .map(Translation::getLanguageCode)
                .filter(l -> l != null && !l.isBlank())
                .map(String::toLowerCase)
                .distinct()
                .toList();

        Map<String, String> statusMap = new HashMap<>();
        for (Translation t : translations) {
            if (t.getLanguageCode() != null) {
                statusMap.put(t.getLanguageCode().toLowerCase(), resolveTranslationStatus(t));
            }
        }

        String activeTransStatus = resolveActiveTransStatus(question, statusMap, targetLang);
        return new TranslationMetadata(translatedLangs, statusMap, activeTransStatus);
    }

    private String resolveActiveTransStatus(Question question, Map<String, String> statusMap, String targetLang) {
        if (targetLang == null || targetLang.isBlank()) {
            return null;
        }
        if ("en".equalsIgnoreCase(targetLang.trim())) {
            return question.getState() != null ? question.getState() : QuestionStateConstants.PUBLISHED;
        }
        return statusMap.getOrDefault(targetLang.toLowerCase(), STATUS_MISSING);
    }

    private String resolveTranslationStatus(Translation t) {
        if (t == null) return STATUS_MISSING;
        if (t.getStatus() == Translation.TranslationStatus.DRAFT) {
            if (t.getReviewComments() != null && !t.getReviewComments().isBlank()) {
                return STATE_REJECTED;
            }
            return QuestionStateConstants.DRAFT;
        }
        return t.getStatus() != null ? t.getStatus().name() : QuestionStateConstants.DRAFT;
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
        if (text.contains("<img") || text.contains("<svg") || text.contains("data:image/")) return true;
        return MARKDOWN_IMAGE_PATTERN.matcher(text).find();
    }

    private int resolveBenchmarkTimeSeconds(Integer requestBenchmark, DifficultyLevel difficulty) {
        if (requestBenchmark != null && requestBenchmark > 0) {
            return requestBenchmark;
        }
        if (difficulty == null) {
            return 90;
        }
        return switch (difficulty) {
            case EASY -> 60;
            case MEDIUM -> 120;
            case HARD -> 180;
        };
    }
}
