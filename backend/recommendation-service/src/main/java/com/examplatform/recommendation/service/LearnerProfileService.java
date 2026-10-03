// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.service;

import com.examplatform.recommendation.domain.LearnerProfile;
import com.examplatform.recommendation.dto.LearnerProfileDto;
import com.examplatform.recommendation.event.PracticeSessionCompletedEvent;
import com.examplatform.recommendation.repository.LearnerProfileRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LearnerProfileService {
    private final LearnerProfileRepository profileRepository;
    private final ObjectMapper objectMapper;
    
    @Transactional
    public LearnerProfile updateProfile(PracticeSessionCompletedEvent event) {
        LearnerProfile profile = profileRepository.findByCandidateId(event.candidateId())
            .orElseGet(() -> LearnerProfile.builder().candidateId(event.candidateId()).build());
        
        // Update session counts
        profile.setTotalPracticeSessions(profile.getTotalPracticeSessions() + 1);
        int qAttempted = event.correctCount() + event.incorrectCount();
        profile.setTotalQuestionsAttempted(profile.getTotalQuestionsAttempted() + qAttempted);
        
        // Update overall accuracy (weighted rolling average)
        int totalSessions = profile.getTotalPracticeSessions();
        double newAccuracy = event.correctCount() + event.incorrectCount() > 0
            ? (double) event.correctCount() / (event.correctCount() + event.incorrectCount())
            : 0.0;
        double currentOverall = profile.getOverallAccuracy();
        double updatedOverall = currentOverall + (newAccuracy - currentOverall) / totalSessions;
        profile.setOverallAccuracy(updatedOverall);
        
        // Update topic accuracy from topicWiseBreakdown JSONB
        updateTopicAccuracy(profile, event.topicWiseBreakdown());
        
        profile.setLastUpdatedAt(Instant.now());
        return profileRepository.save(profile);
    }
    
    private void updateTopicAccuracy(LearnerProfile profile, String topicBreakdownJson) {
        if (topicBreakdownJson == null || topicBreakdownJson.isBlank()) return;
        try {
            // topicBreakdown: {topicName: {correct: N, total: M}}
            Map<String, Map<String, Integer>> breakdown = objectMapper.readValue(
                topicBreakdownJson, new TypeReference<>() {});
            
            Map<String, Map<String, Object>> existingMap = profile.getTopicAccuracyMap() != null
                ? objectMapper.readValue(profile.getTopicAccuracyMap(), new TypeReference<>() {})
                : new HashMap<>();
            
            List<String> weakTopics = new ArrayList<>();
            List<String> strongTopics = new ArrayList<>();
            double weakThreshold = 0.6;
            
            for (Map.Entry<String, Map<String, Integer>> entry : breakdown.entrySet()) {
                String topic = entry.getKey();
                int correct = entry.getValue().getOrDefault("correct", 0);
                int total = entry.getValue().getOrDefault("total", 0);
                double acc = total > 0 ? (double) correct / total : 0.0;
                
                // Merge with existing data
                Map<String, Object> topicData = existingMap.computeIfAbsent(topic, k -> new HashMap<>());
                int existingAttempts = ((Number) topicData.getOrDefault("attempts", 0)).intValue();
                double existingAcc = ((Number) topicData.getOrDefault("accuracy", 0.0)).doubleValue();
                int newAttempts = existingAttempts + total;
                double newAcc = newAttempts > 0
                    ? (existingAcc * existingAttempts + acc * total) / newAttempts
                    : acc;
                
                topicData.put("accuracy", Math.round(newAcc * 1000.0) / 1000.0);
                topicData.put("attempts", newAttempts);
                existingMap.put(topic, topicData);
                
                if (newAcc < weakThreshold) weakTopics.add(topic);
                else strongTopics.add(topic);
            }
            
            profile.setTopicAccuracyMap(objectMapper.writeValueAsString(existingMap));
            profile.setWeakTopics(objectMapper.writeValueAsString(weakTopics));
            profile.setStrongTopics(objectMapper.writeValueAsString(strongTopics));
        } catch (Exception e) {
            // log warning but don't fail the whole event
        }
    }
    
    @Transactional(readOnly = true)
    public LearnerProfileDto getProfile(UUID candidateId) {
        return profileRepository.findByCandidateId(candidateId)
            .map(p -> new LearnerProfileDto(p.getCandidateId(), p.getTotalPracticeSessions(),
                p.getOverallAccuracy(), p.getWeakTopics(), p.getStrongTopics(), p.getTopicAccuracyMap()))
            .orElse(new LearnerProfileDto(candidateId, 0, 0.0, "[]", "[]", "{}"));
    }
}
