// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.service;

import com.examplatform.recommendation.domain.Recommendation;
import com.examplatform.recommendation.dto.RecommendationDto;
import com.examplatform.recommendation.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final RecommendationRepository recommendationRepository;
    
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
}
