// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.service;

import com.examplatform.recommendation.domain.Recommendation;
import com.examplatform.recommendation.dto.RecommendationDto;
import com.examplatform.recommendation.dto.RecommendationResponse;
import com.examplatform.recommendation.dto.WeakTopicItemDto;
import com.examplatform.recommendation.repository.LearnerProfileRepository;
import com.examplatform.recommendation.repository.RecommendationRepository;
import com.examplatform.shared.event.PracticeSessionCompletedEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
public class RecommendationService {
    private final RecommendationRepository recommendationRepository;
    private final ObjectMapper objectMapper;
    private final RecommendationAiService recommendationAiService;
    private final LearnerProfileRepository learnerProfileRepository;
    
    @Transactional(readOnly = true)
    public Optional<RecommendationDto> getLatest(UUID candidateId) {
        return recommendationRepository.findTopByCandidateIdOrderByCreatedAtDesc(candidateId)
            .map(this::toDto);
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
    
    private RecommendationDto toDto(Recommendation r) {
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
