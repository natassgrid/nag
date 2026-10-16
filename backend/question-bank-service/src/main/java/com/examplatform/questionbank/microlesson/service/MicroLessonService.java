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
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class MicroLessonService {

    private final SubtopicMicroLessonRepository repository;
    private final MicroLessonGeneratorService generatorService;
    private final MicroLessonCacheService cacheService;
    private final KaTexValidator kaTexValidator;
    private final ObjectMapper objectMapper;

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
        String targetStatus = switch (rawStatus) {
            case "REJECT", "REJECTED" -> MicroLessonStatus.REJECTED.name();
            case "APPROVE", "APPROVED" -> MicroLessonStatus.APPROVED.name();
            case "PUBLISH", "PUBLISHED" -> MicroLessonStatus.PUBLISHED.name();
            case "EDIT" -> lesson.getStatus() != null ? lesson.getStatus() : MicroLessonStatus.IN_REVIEW.name();
            case "IN_REVIEW" -> MicroLessonStatus.IN_REVIEW.name();
            case "AI_GENERATED" -> MicroLessonStatus.AI_GENERATED.name();
            case "DRAFT" -> MicroLessonStatus.DRAFT.name();
            default -> {
                try {
                    yield MicroLessonStatus.valueOf(rawStatus).name();
                } catch (IllegalArgumentException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + request.status());
                }
            }
        };

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
        List<String> cheatSheet = Collections.emptyList();
        if (lesson.getCheatSheetJson() != null && !lesson.getCheatSheetJson().isBlank()) {
            try {
                cheatSheet = objectMapper.readValue(lesson.getCheatSheetJson(), new TypeReference<>() {});
            } catch (JsonProcessingException ignored) {}
        }
        StepByStepBreakdownDto breakdown = null;
        if (lesson.getStepByStepBreakdownJson() != null && !lesson.getStepByStepBreakdownJson().isBlank()) {
            try {
                breakdown = objectMapper.readValue(lesson.getStepByStepBreakdownJson(), StepByStepBreakdownDto.class);
            } catch (JsonProcessingException ignored) {}
        }

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
        MicroLessonResponse response = generatorService.toResponse(saved);

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
        return generatorService.toResponse(lesson);
    }

    @Transactional(readOnly = true)
    public Optional<MicroLessonResponse> getPublishedMicroLesson(Long subtopicId, String targetExam) {
        // 1. Check Redis cache first
        Optional<MicroLessonResponse> cached = cacheService.getCached(subtopicId);
        if (cached.isPresent()) {
            return cached;
        }

        // 2. Fetch from database if cache miss
        String exam = (targetExam != null && !targetExam.isBlank()) ? targetExam : "JEE_MAIN";
        return repository.findFirstBySubtopicIdAndTargetExamAndStatusOrderByContentVersionDesc(
                        subtopicId, exam, MicroLessonStatus.PUBLISHED.name())
                .or(() -> repository.findFirstBySubtopicIdAndStatusOrderByContentVersionDesc(
                        subtopicId, MicroLessonStatus.PUBLISHED.name()))
                .map(lesson -> {
                    MicroLessonResponse response = generatorService.toResponse(lesson);
                    cacheService.put(subtopicId, response);
                    return response;
                });
    }
}
