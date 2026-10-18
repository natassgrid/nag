// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.service;

import com.examplatform.recommendation.domain.LearnerProfile;
import com.examplatform.recommendation.domain.Recommendation;
import com.examplatform.recommendation.dto.RecommendationDto;
import com.examplatform.recommendation.dto.RecommendationResponse;
import com.examplatform.recommendation.dto.WeakTopicItemDto;
import com.examplatform.recommendation.repository.LearnerProfileRepository;
import com.examplatform.recommendation.repository.LearnerSubtopicPerformanceRepository;
import com.examplatform.recommendation.repository.RecommendationRepository;
import com.examplatform.shared.event.PracticeSessionCompletedEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class RecommendationService {
    private final RecommendationRepository recommendationRepository;
    private final ObjectMapper objectMapper;
    private final RecommendationAiService recommendationAiService;
    private final LearnerProfileRepository learnerProfileRepository;
    private final LearnerProfileService learnerProfileService;
    private final LearnerSubtopicPerformanceRepository subtopicPerformanceRepository;

    @Autowired
    public RecommendationService(
            RecommendationRepository recommendationRepository,
            ObjectMapper objectMapper,
            RecommendationAiService recommendationAiService,
            LearnerProfileRepository learnerProfileRepository,
            @Autowired(required = false) LearnerProfileService learnerProfileService,
            @Autowired(required = false) LearnerSubtopicPerformanceRepository subtopicPerformanceRepository) {
        this.recommendationRepository = recommendationRepository;
        this.objectMapper = objectMapper;
        this.recommendationAiService = recommendationAiService;
        this.learnerProfileRepository = learnerProfileRepository;
        this.learnerProfileService = learnerProfileService;
        this.subtopicPerformanceRepository = subtopicPerformanceRepository;
    }

    public RecommendationService(
            RecommendationRepository recommendationRepository,
            ObjectMapper objectMapper,
            RecommendationAiService recommendationAiService,
            LearnerProfileRepository learnerProfileRepository) {
        this(recommendationRepository, objectMapper, recommendationAiService, learnerProfileRepository, null, null);
    }
    
    @Transactional
    public Optional<RecommendationDto> getLatest(UUID candidateId) {
        Optional<Recommendation> latestOpt = recommendationRepository.findTopByCandidateIdOrderByCreatedAtDesc(candidateId);
        if (latestOpt.isPresent()) {
            Recommendation rec = latestOpt.get();
            if ("PENDING".equals(rec.getStatus()) && (rec.getWeakTopicRecommendations() == null || rec.getStudyPlanItems() == null)) {
                LearnerProfile profile = learnerProfileRepository != null
                        ? learnerProfileRepository.findByCandidateId(candidateId).orElse(null)
                        : null;
                if (profile != null && recommendationAiService != null) {
                    recommendationAiService.populateDeterministicFallback(rec, profile, null);
                    rec = recommendationRepository.save(rec);
                }
            }
            return Optional.of(toDto(rec));
        }

        LearnerProfile profile = learnerProfileRepository != null
                ? learnerProfileRepository.findByCandidateId(candidateId).orElse(null)
                : null;
        boolean hasPracticeHistory = profile != null && profile.getTotalPracticeSessions() > 0;
        if (!hasPracticeHistory && subtopicPerformanceRepository != null) {
            hasPracticeHistory = !subtopicPerformanceRepository.findByCandidateId(candidateId).isEmpty();
        }

        if (hasPracticeHistory && recommendationAiService != null && profile != null) {
            Recommendation rec = recommendationAiService.buildDeterministicRecommendation(profile, null, null);
            return Optional.of(toDto(rec));
        }

        return Optional.empty();
    }

    @Transactional
    public RecommendationDto generateOrRefresh(UUID candidateId, String examTarget) {
        LearnerProfile profile;
        if (learnerProfileService != null) {
            profile = learnerProfileService.getOrCreateProfile(candidateId);
        } else if (learnerProfileRepository != null) {
            profile = learnerProfileRepository.findByCandidateId(candidateId)
                    .orElseGet(() -> learnerProfileRepository.save(LearnerProfile.builder()
                            .candidateId(candidateId)
                            .totalPracticeSessions(0)
                            .totalQuestionsAttempted(0)
                            .overallAccuracy(0.0)
                            .topicAccuracyMap("{}")
                            .subtopicAccuracyMap("{}")
                            .weakTopics("[]")
                            .strongTopics("[]")
                            .build()));
        } else {
            profile = LearnerProfile.builder().candidateId(candidateId).build();
        }

        Recommendation rec = recommendationAiService.generateOnDemand(profile, examTarget);
        return toDto(rec);
    }
    
    @Transactional(readOnly = true)
    public Page<RecommendationDto> getHistory(UUID candidateId, Pageable pageable) {
        return recommendationRepository.findByCandidateIdOrderByCreatedAtDesc(candidateId, pageable)
            .map(this::toDto);
    }
    
    @Transactional
    public void dismiss(UUID id, UUID candidateId) {
        recommendationRepository.findById(id)
            .filter(r -> r.getCandidateId().equals(candidateId))
            .ifPresent(r -> {
                r.setStatus("DISMISSED");
                recommendationRepository.save(r);
            });
    }
    
    public RecommendationDto toDto(Recommendation r) {
        return new RecommendationDto(
            r.getId(),
            r.getCandidateId(),
            r.getTriggerSessionId(),
            r.getStatus(),
            r.getGeneratedAt(),
            r.getWeakTopicRecommendations(),
            r.getStudyPlanItems(),
            r.getSuggestedPracticeSetIds(),
            r.getMotivationalMessage()
        );
    }

    public void generateRecommendationsAsync(PracticeSessionCompletedEvent event) {
        if (learnerProfileRepository != null && recommendationAiService != null) {
            learnerProfileRepository.findByCandidateId(event.candidateId())
                    .ifPresent(profile -> recommendationAiService.generateAndSave(profile, event.sessionId()));
        }
    }

    @Transactional
    public RecommendationResponse updateStudyPlanDay(UUID candidateId, int dayNumber, boolean completed) {
        Recommendation rec = recommendationRepository.findTopByCandidateIdOrderByCreatedAtDesc(candidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No recommendation found for candidate"));

        List<RecommendationResponse.StudyPlanDay> planDays = new ArrayList<>();
        if (rec.getStudyPlanItems() != null && !rec.getStudyPlanItems().isBlank()) {
            try {
                planDays = objectMapper.readValue(rec.getStudyPlanItems(), new TypeReference<List<RecommendationResponse.StudyPlanDay>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse studyPlanItems for recommendation {}: {}", rec.getId(), e.getMessage());
            }
        }

        boolean found = false;
        List<RecommendationResponse.StudyPlanDay> updated = new ArrayList<>();
        for (RecommendationResponse.StudyPlanDay day : planDays) {
            if (day.day() == dayNumber) {
                updated.add(new RecommendationResponse.StudyPlanDay(
                        day.day(),
                        day.focusTopic(),
                        day.tasks(),
                        day.durationMinutes(),
                        completed
                ));
                found = true;
            } else {
                updated.add(day);
            }
        }

        if (!found) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Study plan day " + dayNumber + " not found");
        }

        try {
            rec.setStudyPlanItems(objectMapper.writeValueAsString(updated));
            recommendationRepository.save(rec);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize updated study plan", e);
        }

        return toResponse(rec);
    }

    public RecommendationResponse toResponse(Recommendation r) {
        List<WeakTopicItemDto> weakTopics = Collections.emptyList();
        if (r.getWeakTopicRecommendations() != null && !r.getWeakTopicRecommendations().isBlank()) {
            try {
                weakTopics = objectMapper.readValue(r.getWeakTopicRecommendations(), new TypeReference<List<WeakTopicItemDto>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse weakTopicRecommendations: {}", e.getMessage());
            }
        }

        List<RecommendationResponse.StudyPlanDay> studyPlan = Collections.emptyList();
        if (r.getStudyPlanItems() != null && !r.getStudyPlanItems().isBlank()) {
            try {
                studyPlan = objectMapper.readValue(r.getStudyPlanItems(), new TypeReference<List<RecommendationResponse.StudyPlanDay>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse studyPlanItems: {}", e.getMessage());
            }
        }

        List<String> suggestedSets = Collections.emptyList();
        if (r.getSuggestedPracticeSetIds() != null && !r.getSuggestedPracticeSetIds().isBlank()) {
            try {
                suggestedSets = objectMapper.readValue(r.getSuggestedPracticeSetIds(), new TypeReference<List<String>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse suggestedPracticeSetIds: {}", e.getMessage());
            }
        }

        return new RecommendationResponse(
                r.getId(),
                r.getCandidateId(),
                r.getTriggerSessionId(),
                r.getStatus(),
                r.getGeneratedAt(),
                weakTopics,
                studyPlan,
                suggestedSets,
                r.getMotivationalMessage()
        );
    }
}
