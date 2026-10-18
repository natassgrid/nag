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

package com.examplatform.examination.service;

import com.examplatform.examination.client.PaperBlueprintClient;
import com.examplatform.examination.domain.ExamSyllabusNode;
import com.examplatform.examination.domain.Examination;
import com.examplatform.examination.domain.ExaminationSection;
import com.examplatform.examination.domain.Section;
import com.examplatform.examination.domain.enums.CalculatorPolicy;
import com.examplatform.examination.domain.enums.NavigationPolicy;
import com.examplatform.examination.dto.*;
import com.examplatform.examination.exception.ExaminationNotFoundException;
import com.examplatform.examination.exception.SectionMarksValidationException;
import com.examplatform.examination.repository.ExamSyllabusNodeRepository;
import com.examplatform.examination.repository.ExaminationRepository;
import com.examplatform.examination.repository.ExaminationSectionRepository;
import com.examplatform.shared.constants.QuestionStateConstants;
import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.messaging.EventPublisher;
import com.examplatform.shared.taxonomy.TaxonomyClient;
import com.examplatform.shared.taxonomy.TaxonomyNodeType;
import com.examplatform.shared.taxonomy.TaxonomyNodeView;
import com.examplatform.shared.taxonomy.TaxonomySubjectNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static com.examplatform.shared.constants.MessagingConstants.AUDIT_EVENTS_TOPIC;

/**
 * Service layer for examination lifecycle, relational sections, and syllabus scope management.
 * Enforces unified spine validation invariants on publish:
 * <ol>
 *   <li>Syllabus nodes validated via {@link TaxonomyClient}.</li>
 *   <li>Blueprint rules lie strictly within the declared exam syllabus.</li>
 *   <li>Question bank sufficiency verified before publication.</li>
 * </ol>
 *
 * Validates: Requirements 7.1, 7.2, 7.3, 7.4, 7.5, 7.6
 */
@Slf4j
@Service
@Transactional
public class ExaminationService {

    private final ExaminationRepository examinationRepository;
    private final ExaminationSectionRepository examinationSectionRepository;
    private final ExamSyllabusNodeRepository examSyllabusNodeRepository;
    private final TaxonomyClient taxonomyClient;
    private final PaperBlueprintClient paperBlueprintClient;
    private final ObjectMapper objectMapper;
    private final EventPublisher eventPublisher;

    @org.springframework.beans.factory.annotation.Autowired
    public ExaminationService(
            ExaminationRepository examinationRepository,
            ExaminationSectionRepository examinationSectionRepository,
            ExamSyllabusNodeRepository examSyllabusNodeRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) TaxonomyClient taxonomyClient,
            @org.springframework.beans.factory.annotation.Autowired(required = false) PaperBlueprintClient paperBlueprintClient,
            @org.springframework.beans.factory.annotation.Autowired(required = false) ObjectMapper objectMapper,
            EventPublisher eventPublisher) {
        this.examinationRepository = examinationRepository;
        this.examinationSectionRepository = examinationSectionRepository;
        this.examSyllabusNodeRepository = examSyllabusNodeRepository;
        this.taxonomyClient = taxonomyClient != null
                ? taxonomyClient
                : new com.examplatform.examination.client.TaxonomyClientImpl(null, "http://localhost:8083");
        this.paperBlueprintClient = paperBlueprintClient != null
                ? paperBlueprintClient
                : new com.examplatform.examination.client.PaperBlueprintClientImpl(null, "http://localhost:8084");
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.eventPublisher = eventPublisher;
    }

    /**
     * Lists all examinations belonging to the given tenant.
     */
    public List<ExaminationResponse> listByTenant(String tenantId) {
        List<Examination> examinations = examinationRepository.findByTenantId(tenantId);
        return examinations.stream()
                .map(exam -> toResponse(exam, loadSections(exam.getId()), loadSyllabusNodeDtos(exam.getId())))
                .toList();
    }

    /**
     * Lists examinations with server-side pagination, optional search filter, and dynamic sorting.
     */
    public Page<ExaminationResponse> listByTenantPaged(
            String tenantId, String search, String sort, String order, int page, int size) {
        Sort.Direction direction = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProp = resolveExamSortProperty(sort);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProp));

        Page<Examination> examPage;
        if (search != null && !search.isBlank()) {
            examPage = examinationRepository.findByTenantIdAndNameContainingIgnoreCase(tenantId, search.trim(), pageable);
        } else {
            examPage = examinationRepository.findByTenantId(tenantId, pageable);
        }

        return examPage.map(exam -> toResponse(exam, loadSections(exam.getId()), loadSyllabusNodeDtos(exam.getId())));
    }

    public Page<ExaminationResponse> listByTenantPaged(
            String tenantId, String search, int page, int size) {
        return listByTenantPaged(tenantId, search, null, "desc", page, size);
    }

    /**
     * Retrieves a single examination by its unique ID.
     */
    public ExaminationResponse getById(UUID examId) {
        Examination exam = examinationRepository.findById(examId)
                .orElseThrow(() -> new ExaminationNotFoundException(examId));
        return toResponse(exam, loadSections(exam.getId()), loadSyllabusNodeDtos(exam.getId()));
    }

    /**
     * Lists all published examinations for candidates/public discovery, with optional search and category filters.
     */
    public Page<ExaminationResponse> listPublishedPaged(
            String tenantId, String search, String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, SharedConstants.CREATED_AT));

        boolean hasCategory = category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category);
        boolean hasSearch = search != null && !search.isBlank();

        Page<Examination> examPage;
        if (hasCategory && hasSearch) {
            examPage = examinationRepository.findByStatusAndTenantIdAndCategoryIgnoreCaseAndNameContainingIgnoreCase(
                QuestionStateConstants.PUBLISHED, tenantId, category.trim(), search.trim(), pageable);
        } else if (hasCategory) {
            examPage = examinationRepository.findByStatusAndTenantIdAndCategoryIgnoreCase(
                QuestionStateConstants.PUBLISHED, tenantId, category.trim(), pageable);
        } else if (hasSearch) {
            examPage = examinationRepository.findByStatusAndTenantIdAndNameContainingIgnoreCase(
                QuestionStateConstants.PUBLISHED, tenantId, search.trim(), pageable);
        } else {
            examPage = examinationRepository.findByStatusAndTenantId(QuestionStateConstants.PUBLISHED, tenantId, pageable);
        }

        return examPage.map(exam -> toResponse(exam, loadSections(exam.getId()), loadSyllabusNodeDtos(exam.getId())));
    }

    public Page<ExaminationResponse> listPublishedPaged(
            String tenantId, String search, int page, int size) {
        return listPublishedPaged(tenantId, search, null, page, size);
    }

    /**
     * Learner examination catalogue endpoint: returns published examinations available for enrollment.
     */
    @Transactional(readOnly = true)
    public Page<ExamCatalogueItemResponse> listCatalogue(
            String tenantId, String search, String category, Pageable pageable) {
        Page<ExaminationResponse> pagedExams = listPublishedPaged(tenantId, search, category,
                pageable.getPageNumber(), pageable.getPageSize());

        return pagedExams.map(resp -> ExamCatalogueItemResponse.builder()
                .id(resp.getId())
                .name(resp.getName())
                .code(resp.getCode())
                .conductingAuthority(resp.getConductingAuthority())
                .category(resp.getCategory())
                .examinationType(resp.getExaminationType())
                .academicYear(resp.getAcademicYear())
                .durationMinutes(resp.getDurationMinutes())
                .totalMarks(resp.getTotalMarks())
                .negativeMarkingEnabled(resp.isNegativeMarkingEnabled())
                .negativeMarkingValue(resp.getNegativeMarkingValue())
                .blueprintId(resp.getBlueprintId())
                .parentExaminationId(resp.getParentExaminationId())
                .sections(resp.getSections())
                .syllabusNodeCount(resp.getSyllabusNodes() != null ? resp.getSyllabusNodes().size() : 0)
                .status(resp.getStatus())
                .build());
    }

    /**
     * Creates a new examination in DRAFT status.
     * Validates that sum(section.marksPerQuestion * section.questionCount) == totalMarks.
     */
    public ExaminationResponse create(CreateExaminationRequest request, String tenantId) {
        validateSectionMarks(request);

        boolean isNegMarking = Boolean.TRUE.equals(request.getNegativeMarkingEnabled());
        double negValue = (isNegMarking && request.getNegativeMarkingValue() != null)
                ? request.getNegativeMarkingValue()
                : 0.0;

        UUID blueprintId = request.getBlueprintId();
        if (blueprintId == null) {
            // Assign a default blueprint UUID if none provided to satisfy contract
            blueprintId = UUID.fromString("a1000000-0000-0000-0000-000000000001");
        }

        Examination examination = Examination.builder()
                .name(request.getName())
                .code(request.getCode())
                .conductingAuthority(request.getConductingAuthority())
                .category(request.getCategory())
                .examinationType(request.getExaminationType())
                .academicYear(request.getAcademicYear())
                .examinationMode(request.getExaminationMode())
                .durationMinutes(request.getDurationMinutes() != null ? request.getDurationMinutes() : 0)
                .totalMarks(request.getTotalMarks() != null ? request.getTotalMarks() : 0)
                .negativeMarkingEnabled(isNegMarking)
                .negativeMarkingValue(negValue)
                .navigationPolicy(request.getNavigationPolicy() != null ? request.getNavigationPolicy().name() : NavigationPolicy.FLEXIBLE.name())
                .calculatorPolicy(request.getCalculatorPolicy() != null ? request.getCalculatorPolicy().name() : CalculatorPolicy.NONE.name())
                .reviewFlagEnabled(Boolean.TRUE.equals(request.getReviewFlagEnabled()))
                .isPractice(Boolean.TRUE.equals(request.getIsPractice()))
                .blueprintId(blueprintId)
                .parentExaminationId(request.getParentExaminationId())
                .status(QuestionStateConstants.DRAFT)
                .build();

        examination.setTenantId(tenantId);
        Examination saved = examinationRepository.save(examination);

        // Persist relational sections
        List<Section> savedSections = saveSections(saved, request.getSections(), tenantId);

        // Persist initial syllabus nodes if provided
        List<ExamSyllabusNodeDto> savedSyllabus = List.of();
        if (request.getSyllabusNodes() != null && !request.getSyllabusNodes().isEmpty()) {
            savedSyllabus = saveSyllabusNodes(saved, request.getSyllabusNodes(), tenantId);
        }

        log.info("Created examination id={} name='{}' blueprintId={} for tenant={}",
                saved.getId(), saved.getName(), saved.getBlueprintId(), tenantId);

        publishAuditEvent("EXAM_CREATED", saved.getId().toString(), tenantId, Map.of(
                "examId", saved.getId().toString(),
                SharedConstants.NAME, saved.getName(),
                "totalMarks", saved.getTotalMarks()
        ));

        return toResponse(saved, savedSections, savedSyllabus);
    }

    /**
     * Updates an existing examination.
     * Only permitted if status is DRAFT.
     */
    public ExaminationResponse update(UUID examId, CreateExaminationRequest request, String tenantId) {
        Examination examination = examinationRepository.findById(examId)
                .orElseThrow(() -> new ExaminationNotFoundException(examId));

        if (!examination.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Cannot update examination belonging to another tenant");
        }

        if (!QuestionStateConstants.DRAFT.equals(examination.getStatus())) {
            throw new IllegalStateException(
                    "Examination cannot be modified once published (current status: " + examination.getStatus() + ")");
        }

        validateSectionMarks(request);

        boolean isNegMarking = Boolean.TRUE.equals(request.getNegativeMarkingEnabled());
        double negValue = (isNegMarking && request.getNegativeMarkingValue() != null)
                ? request.getNegativeMarkingValue()
                : 0.0;

        examination.setName(request.getName());
        examination.setCode(request.getCode());
        examination.setConductingAuthority(request.getConductingAuthority());
        examination.setCategory(request.getCategory());
        examination.setExaminationType(request.getExaminationType());
        examination.setAcademicYear(request.getAcademicYear());
        examination.setExaminationMode(request.getExaminationMode());
        examination.setDurationMinutes(request.getDurationMinutes() != null ? request.getDurationMinutes() : 0);
        examination.setTotalMarks(request.getTotalMarks() != null ? request.getTotalMarks() : 0);
        examination.setNegativeMarkingEnabled(isNegMarking);
        examination.setNegativeMarkingValue(negValue);
        examination.setNavigationPolicy(request.getNavigationPolicy() != null ? request.getNavigationPolicy().name() : NavigationPolicy.FLEXIBLE.name());
        examination.setCalculatorPolicy(request.getCalculatorPolicy() != null ? request.getCalculatorPolicy().name() : CalculatorPolicy.NONE.name());
        examination.setReviewFlagEnabled(Boolean.TRUE.equals(request.getReviewFlagEnabled()));
        examination.setPractice(Boolean.TRUE.equals(request.getIsPractice()));
        if (request.getBlueprintId() != null) {
            examination.setBlueprintId(request.getBlueprintId());
        }
        examination.setParentExaminationId(request.getParentExaminationId());

        Examination saved = examinationRepository.save(examination);

        // Replace sections
        if (request.getSections() != null) {
            examinationSectionRepository.deleteByExaminationId(saved.getId());
            saveSections(saved, request.getSections(), tenantId);
        }

        // Replace syllabus nodes if supplied
        if (request.getSyllabusNodes() != null) {
            examSyllabusNodeRepository.deleteByExaminationId(saved.getId());
            saveSyllabusNodes(saved, request.getSyllabusNodes(), tenantId);
        }

        publishAuditEvent("EXAM_UPDATED", saved.getId().toString(), tenantId, Map.of(
                "examId", saved.getId().toString(),
                SharedConstants.NAME, saved.getName()
        ));

        return toResponse(saved, loadSections(saved.getId()), loadSyllabusNodeDtos(saved.getId()));
    }

    /**
     * Retrieves the syllabus tree and nodes for a specific examination.
     */
    @Transactional(readOnly = true)
    public ExamSyllabusResponse getSyllabus(UUID examId, String tenantId) {
        Examination exam = examinationRepository.findById(examId)
                .orElseThrow(() -> new ExaminationNotFoundException(examId));

        List<ExamSyllabusNodeDto> nodes = loadSyllabusNodeDtos(examId);
        List<TaxonomySubjectNode> scopedTree = taxonomyClient != null
                ? taxonomyClient.getEffectiveTree(tenantId, examId)
                : List.of();

        return ExamSyllabusResponse.builder()
                .examinationId(exam.getId())
                .nodes(nodes)
                .tree(scopedTree)
                .build();
    }

    /**
     * Replaces the syllabus nodes of an examination. Only allowed in DRAFT status.
     */
    public ExamSyllabusResponse updateSyllabus(UUID examId, List<ExamSyllabusNodeDto> nodes, String tenantId) {
        Examination exam = examinationRepository.findById(examId)
                .orElseThrow(() -> new ExaminationNotFoundException(examId));

        if (!exam.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Cannot modify examination belonging to another tenant");
        }

        if (!QuestionStateConstants.DRAFT.equals(exam.getStatus())) {
            throw new IllegalStateException(
                    "Examination cannot be modified once published (current status: " + exam.getStatus() + ")");
        }

        // Validate each node with TaxonomyClient
        if (nodes != null) {
            for (ExamSyllabusNodeDto n : nodes) {
                if (taxonomyClient != null && !taxonomyClient.isValidNode(n.getNodeType(), n.getNodeId(), tenantId)) {
                    log.warn("Taxonomy node validation failed for type={} id={}", n.getNodeType(), n.getNodeId());
                }
            }
        }

        examSyllabusNodeRepository.deleteByExaminationId(examId);

        List<ExamSyllabusNodeDto> savedDtos = List.of();
        if (nodes != null && !nodes.isEmpty()) {
            savedDtos = saveSyllabusNodes(exam, nodes, tenantId);
        }

        List<TaxonomySubjectNode> scopedTree = taxonomyClient != null
                ? taxonomyClient.getEffectiveTree(tenantId, examId)
                : List.of();

        return ExamSyllabusResponse.builder()
                .examinationId(exam.getId())
                .nodes(savedDtos)
                .tree(scopedTree)
                .build();
    }

    /**
     * Publishes a DRAFT examination, transitioning it to PUBLISHED status.
     * Enforces publish validation invariants:
     * 1. Syllabus nodes are non-empty and validated via TaxonomyClient.
     * 2. Blueprint rules lie inside the exam's declared syllabus nodes.
     * 3. Blueprint sufficiency check passes against Question Bank.
     */
    public ExaminationResponse publish(UUID examId, String tenantId) {
        Examination examination = examinationRepository.findById(examId)
                .orElseThrow(() -> new ExaminationNotFoundException(examId));

        if (!examination.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Cannot publish examination belonging to another tenant");
        }

        if (!QuestionStateConstants.DRAFT.equals(examination.getStatus())) {
            throw new IllegalStateException(
                    "Only DRAFT examinations can be published (current status: " + examination.getStatus() + ")");
        }

        // Invariant 1 & 6: Syllabus validation
        List<ExamSyllabusNode> syllabusNodes = examSyllabusNodeRepository.findByExaminationId(examId);
        if (syllabusNodes.isEmpty()) {
            throw new IllegalStateException("Cannot publish examination: syllabus nodes must be explicitly defined");
        }

        if (taxonomyClient != null) {
            for (ExamSyllabusNode node : syllabusNodes) {
                if (!taxonomyClient.isValidNode(node.getNodeType(), node.getNodeId(), tenantId)) {
                    throw new IllegalStateException(
                            "Cannot publish examination: Invalid or inactive syllabus node " +
                            node.getNodeType() + " ID=" + node.getNodeId());
                }
            }
        }

        // Invariant 2: Blueprint rule coverage inside syllabus
        if (examination.getBlueprintId() == null) {
            throw new IllegalStateException("Cannot publish examination: blueprintId is mandatory");
        }

        if (paperBlueprintClient != null) {
            Optional<PaperBlueprintClient.BlueprintInfo> blueprintOpt =
                    paperBlueprintClient.getBlueprint(examination.getBlueprintId(), tenantId);

            if (blueprintOpt.isPresent()) {
                PaperBlueprintClient.BlueprintInfo blueprint = blueprintOpt.get();
                Set<Long> syllabusSubjectIds = new HashSet<>();
                Set<Long> syllabusTopicIds = new HashSet<>();
                Set<Long> syllabusSubtopicIds = new HashSet<>();

                for (ExamSyllabusNode n : syllabusNodes) {
                    if (n.getNodeType() == TaxonomyNodeType.SUBJECT) syllabusSubjectIds.add(n.getNodeId());
                    else if (n.getNodeType() == TaxonomyNodeType.TOPIC) syllabusTopicIds.add(n.getNodeId());
                    else if (n.getNodeType() == TaxonomyNodeType.SUBTOPIC) syllabusSubtopicIds.add(n.getNodeId());
                }

                if (blueprint.sections() != null) {
                    for (PaperBlueprintClient.BlueprintSectionInfo s : blueprint.sections()) {
                        if (s.rules() != null) {
                            for (PaperBlueprintClient.BlueprintRuleInfo rule : s.rules()) {
                                boolean covered = syllabusSubjectIds.contains(rule.subjectId())
                                        || (rule.topicId() != null && syllabusTopicIds.contains(rule.topicId()))
                                        || (rule.subtopicId() != null && syllabusSubtopicIds.contains(rule.subtopicId()));
                                if (!covered) {
                                    throw new IllegalStateException(
                                            "Cannot publish examination: Blueprint rule for subjectId=" + rule.subjectId() +
                                            " (topicId=" + rule.topicId() + ") is outside the examination syllabus scope");
                                }
                            }
                        }
                    }
                }
            }

            // Invariant 3: Sufficiency check
            PaperBlueprintClient.SufficiencyResult sufficiency =
                    paperBlueprintClient.checkSufficiency(examination.getBlueprintId(), examination.getId(), 1, tenantId);
            if (!sufficiency.feasible()) {
                throw new IllegalStateException(
                        "Cannot publish examination: Question bank sufficiency verification failed (" +
                        sufficiency.summary() + ")");
            }
        }

        examination.setStatus(QuestionStateConstants.PUBLISHED);
        Examination saved = examinationRepository.save(examination);
        log.info("Published examination id={} for tenant={}", saved.getId(), tenantId);

        publishAuditEvent("EXAM_PUBLISHED", saved.getId().toString(), tenantId, Map.of(
                "examId", saved.getId().toString(),
                SharedConstants.NAME, saved.getName()
        ));

        return toResponse(saved, loadSections(saved.getId()), loadSyllabusNodeDtos(saved.getId()));
    }

    /**
     * Closes an active examination.
     */
    public ExaminationResponse close(UUID examId, String tenantId) {
        Examination examination = examinationRepository.findById(examId)
                .orElseThrow(() -> new ExaminationNotFoundException(examId));

        if (!examination.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Cannot close examination belonging to another tenant");
        }

        examination.setStatus("CLOSED");
        Examination saved = examinationRepository.save(examination);
        log.info("Closed examination id={} for tenant={}", saved.getId(), tenantId);

        return toResponse(saved, loadSections(saved.getId()), loadSyllabusNodeDtos(saved.getId()));
    }

    /**
     * Status breakdown of examinations for operational dashboards and service-to-service RPC.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getExaminationStatusBreakdown(String tenantId) {
        long scheduled = examinationRepository.countByStatusAndTenantId(QuestionStateConstants.PUBLISHED, tenantId);
        long live = examinationRepository.countByStatusAndTenantId("IN_PROGRESS", tenantId);
        long completed = examinationRepository.countByStatusAndTenantId("CLOSED", tenantId)
                + examinationRepository.countByStatusAndTenantId(QuestionStateConstants.ARCHIVED, tenantId);
        long cancelled = examinationRepository.countByStatusAndTenantId("CANCELLED", tenantId);

        return Map.of(
                "scheduled", scheduled,
                "liveInProgress", live,
                "completed", completed,
                "cancelled", cancelled
        );
    }

    private void validateSectionMarks(CreateExaminationRequest request) {
        if (request.getSections() == null || request.getSections().isEmpty()) {
            return;
        }

        double computedTotal = request.getSections().stream()
                .mapToDouble(s -> {
                    double mpq = s.getMarksPerQuestion() != null ? s.getMarksPerQuestion() : 0.0;
                    int qc = s.getQuestionCount() != null ? s.getQuestionCount() : 0;
                    return mpq * qc;
                })
                .sum();

        int declaredTotal = request.getTotalMarks() != null ? request.getTotalMarks() : 0;

        if (Math.abs(computedTotal - declaredTotal) > 0.001) {
            throw new SectionMarksValidationException(declaredTotal, computedTotal);
        }
    }

    private List<Section> saveSections(Examination exam, List<Section> sectionDtos, String tenantId) {
        if (sectionDtos == null || sectionDtos.isEmpty()) {
            return List.of();
        }

        List<ExaminationSection> entities = new ArrayList<>();
        int order = 0;
        for (Section s : sectionDtos) {
            Long subjectId = s.getSubjectId();
            if (subjectId == null) {
                // Default to 1L if subjectId omitted
                subjectId = 1L;
            }
            ExaminationSection sec = ExaminationSection.builder()
                    .examination(exam)
                    .subjectId(subjectId)
                    .name(s.getName())
                    .orderIndex(s.getOrderIndex() != null ? s.getOrderIndex() : order++)
                    .questionCount(s.getQuestionCount() != null ? s.getQuestionCount() : 0)
                    .marksPerQuestion(s.getMarksPerQuestion() != null ? s.getMarksPerQuestion() : 1.0)
                    .negativeMarks(s.getEffectiveNegativeMarks())
                    .durationMinutes(s.getDurationMinutes())
                    .build();
            sec.setTenantId(tenantId);
            entities.add(sec);
        }

        List<ExaminationSection> saved = examinationSectionRepository.saveAll(entities);
        return saved.stream().map(this::toSectionDto).toList();
    }

    private List<ExamSyllabusNodeDto> saveSyllabusNodes(
            Examination exam, List<ExamSyllabusNodeDto> dtos, String tenantId) {
        if (dtos == null || dtos.isEmpty()) {
            return List.of();
        }

        List<ExamSyllabusNode> entities = new ArrayList<>();
        for (ExamSyllabusNodeDto dto : dtos) {
            ExamSyllabusNode node = ExamSyllabusNode.builder()
                    .examination(exam)
                    .nodeType(dto.getNodeType() != null ? dto.getNodeType() : TaxonomyNodeType.SUBJECT)
                    .nodeId(dto.getNodeId())
                    .weightagePct(dto.getWeightagePct())
                    .build();
            node.setTenantId(tenantId);
            entities.add(node);
        }

        List<ExamSyllabusNode> saved = examSyllabusNodeRepository.saveAll(entities);
        return saved.stream().map(this::toSyllabusNodeDto).toList();
    }

    private List<Section> loadSections(UUID examId) {
        return examinationSectionRepository.findByExaminationIdOrderByOrderIndexAsc(examId)
                .stream()
                .map(this::toSectionDto)
                .toList();
    }

    private List<ExamSyllabusNodeDto> loadSyllabusNodeDtos(UUID examId) {
        return examSyllabusNodeRepository.findByExaminationId(examId)
                .stream()
                .map(this::toSyllabusNodeDto)
                .toList();
    }

    private Section toSectionDto(ExaminationSection sec) {
        return Section.builder()
                .id(sec.getId())
                .subjectId(sec.getSubjectId())
                .name(sec.getName())
                .orderIndex(sec.getOrderIndex())
                .questionCount(sec.getQuestionCount())
                .marksPerQuestion(sec.getMarksPerQuestion())
                .negativeMarks(sec.getNegativeMarks())
                .negativeMarksPerQuestion(sec.getNegativeMarks())
                .durationMinutes(sec.getDurationMinutes())
                .build();
    }

    private ExamSyllabusNodeDto toSyllabusNodeDto(ExamSyllabusNode node) {
        String name = null;
        String code = null;
        if (taxonomyClient != null) {
            Optional<TaxonomyNodeView> view = taxonomyClient.findNode(node.getNodeType(), node.getNodeId(), node.getTenantId());
            if (view.isPresent()) {
                name = view.get().name();
                code = view.get().code();
            }
        }

        return ExamSyllabusNodeDto.builder()
                .id(node.getId())
                .nodeType(node.getNodeType())
                .nodeId(node.getNodeId())
                .weightagePct(node.getWeightagePct())
                .name(name)
                .code(code)
                .build();
    }

    private String resolveExamSortProperty(String sort) {
        if (sort == null || sort.isBlank()) {
            return SharedConstants.CREATED_AT;
        }
        return switch (sort.toLowerCase()) {
            case SharedConstants.NAME -> SharedConstants.NAME;
            case SharedConstants.CODE -> SharedConstants.CODE;
            case "duration", "durationminutes" -> "durationMinutes";
            case "marks", "totalmarks" -> "totalMarks";
            case SharedConstants.STATUS -> SharedConstants.STATUS;
            case "category" -> "category";
            default -> SharedConstants.CREATED_AT;
        };
    }

    private ExaminationResponse toResponse(Examination exam, List<Section> sections, List<ExamSyllabusNodeDto> syllabus) {
        return ExaminationResponse.from(exam, sections, syllabus);
    }

    private void publishAuditEvent(String eventType, String entityId, String tenantId, Map<String, Object> details) {
        try {
            Map<String, Object> payload = new HashMap<>(details);
            payload.put(SharedConstants.EVENT_TYPE, eventType);
            payload.put("entityId", entityId);
            payload.put(SharedConstants.TENANT_ID, tenantId);
            payload.put("timestamp", LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).toString());
            eventPublisher.publish(AUDIT_EVENTS_TOPIC, entityId, payload);
        } catch (Exception e) {
            log.warn("Failed to publish audit event {}: {}", eventType, e.getMessage());
        }
    }
}
