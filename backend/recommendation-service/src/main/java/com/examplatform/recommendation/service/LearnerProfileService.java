// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.service;

import com.examplatform.recommendation.domain.LearnerProfile;
import com.examplatform.recommendation.domain.LearnerSubtopicPerformance;
import com.examplatform.recommendation.dto.LearnerProfileDto;
import com.examplatform.shared.event.PracticeSessionCompletedEvent;
import com.examplatform.recommendation.repository.LearnerProfileRepository;
import com.examplatform.recommendation.repository.LearnerSubtopicPerformanceRepository;
import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.dto.SubtopicPerformanceDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LearnerProfileService {
    private static final Logger log = LoggerFactory.getLogger(LearnerProfileService.class);

    private final LearnerProfileRepository profileRepository;
    private final LearnerSubtopicPerformanceRepository subtopicRepository;
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

        // Update granular subtopic telemetry & rolling history
        updateSubtopicPerformance(profile, event.subtopicWiseBreakdown(), event.candidateId());

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
            }

            if (!existingMap.isEmpty()) {
                List<String> weakTopics = new ArrayList<>();
                List<String> strongTopics = new ArrayList<>();
                for (Map.Entry<String, Map<String, Object>> entry : existingMap.entrySet()) {
                    String topic = entry.getKey();
                    double topicAcc = ((Number) entry.getValue().getOrDefault("accuracy", 0.0)).doubleValue();
                    double accNorm = topicAcc <= 1.0 ? topicAcc * 100.0 : topicAcc;
                    if (accNorm < 60.0) {
                        weakTopics.add(topic);
                    } else if (accNorm >= 75.0) {
                        strongTopics.add(topic);
                    }
                }

                profile.setTopicAccuracyMap(objectMapper.writeValueAsString(existingMap));
                profile.setWeakTopics(objectMapper.writeValueAsString(weakTopics));
                profile.setStrongTopics(objectMapper.writeValueAsString(strongTopics));
            }
        } catch (Exception e) {
            log.warn("Failed to update topic accuracy: {}", e.getMessage());
        }
    }

    private void updateSubtopicPerformance(LearnerProfile profile, String subtopicBreakdownJson, UUID candidateId) {
        if (subtopicBreakdownJson == null || subtopicBreakdownJson.isBlank()) return;
        try {
            List<SubtopicPerformanceDto> list = objectMapper.readValue(
                    subtopicBreakdownJson, new TypeReference<>() {
                });

            Map<String, Map<String, Object>> subtopicMap = profile.getSubtopicAccuracyMap() != null
                    ? objectMapper.readValue(profile.getSubtopicAccuracyMap(), new TypeReference<>() {})
                    : new HashMap<>();

            List<LearnerSubtopicPerformance> savedPerfs = new ArrayList<>();

            for (SubtopicPerformanceDto dto : list) {
                if (dto.subtopicName() == null || dto.subtopicName().isBlank()) continue;

                LearnerSubtopicPerformance perf = subtopicRepository
                        .findByCandidateIdAndSubtopicName(candidateId, dto.subtopicName())
                        .orElseGet(() -> {
                            LearnerSubtopicPerformance p = LearnerSubtopicPerformance.builder()
                                    .candidateId(candidateId)
                                    .subjectId(dto.subjectId())
                                    .subjectName(dto.subjectName() != null ? dto.subjectName() :
                                        SharedConstants.GENERAL)
                                    .topicId(dto.topicId())
                                    .topicName(dto.topicName() != null ? dto.topicName() : SharedConstants.GENERAL)
                                    .subtopicId(dto.subtopicId())
                                    .subtopicName(dto.subtopicName())
                                    .build();
                            p.setTenantId(profile.getTenantId() != null ? profile.getTenantId() : SharedConstants.DEFAULT_TENANT);
                            return p;
                        });

                int oldAttempts = perf.getTotalAttempts();
                double oldRollingAccuracy = perf.getAvgAccuracyPercent();

                int newAttempts = oldAttempts + dto.attemptedCount();
                int newCorrect = perf.getTotalCorrect() + dto.correctCount();

                perf.setTotalQuestions(perf.getTotalQuestions() + dto.totalQuestions());
                perf.setTotalAttempts(newAttempts);
                perf.setTotalCorrect(newCorrect);
                perf.setTotalSkipped(perf.getTotalSkipped() + dto.skippedCount());

                double rollingAcc;
                if (oldAttempts > 0 && dto.attemptedCount() > 0) {
                    double cumulativeAcc = (newCorrect * 100.0) / newAttempts;
                    double sessionAcc = (dto.correctCount() * 100.0) / dto.attemptedCount();
                    double ewmaAcc = (0.6 * sessionAcc) + (0.4 * oldRollingAccuracy);
                    rollingAcc = Math.max(cumulativeAcc, ewmaAcc);
                } else if (newAttempts > 0) {
                    rollingAcc = (double) newCorrect * 100.0 / newAttempts;
                } else {
                    rollingAcc = 0.0;
                }
                perf.setAvgAccuracyPercent(Math.round(rollingAcc * 100.0) / 100.0);

                // Exponentially weighted moving average for pace ratio (alpha = 0.3)
                double currentPace = perf.getAvgPaceRatio() > 0 ? perf.getAvgPaceRatio() : 1.0;
                double newPace = dto.paceRatio() > 0 ? dto.paceRatio() : 1.0;
                double updatedPace = (currentPace * 0.7) + (newPace * 0.3);
                perf.setAvgPaceRatio(Math.round(updatedPace * 100.0) / 100.0);

                double rollingSkip = perf.getTotalQuestions() > 0
                        ? (double) perf.getTotalSkipped() * 100.0 / perf.getTotalQuestions()
                        : 0.0;
                perf.setAvgSkipRatePercent(Math.round(rollingSkip * 100.0) / 100.0);

                perf.setLastTestedAt(Instant.now());
                subtopicRepository.save(perf);
                savedPerfs.add(perf);

                // Update subtopic map on profile for rapid JSON querying
                Map<String, Object> subData = subtopicMap.computeIfAbsent(dto.subtopicName(), k -> new HashMap<>());
                subData.put("accuracy", perf.getAvgAccuracyPercent());
                subData.put("attempts", perf.getTotalAttempts());
                subData.put("paceRatio", perf.getAvgPaceRatio());
                subData.put("skipRate", perf.getAvgSkipRatePercent());
                subData.put("topic", perf.getTopicName());
                subData.put("subject", perf.getSubjectName());
            }

            profile.setSubtopicAccuracyMap(objectMapper.writeValueAsString(subtopicMap));

            // Rollup topic accuracy if topicAccuracyMap is missing or empty
            boolean topicMapEmpty = profile.getTopicAccuracyMap() == null
                    || profile.getTopicAccuracyMap().isBlank()
                    || profile.getTopicAccuracyMap().trim().equals("{}");

            if (topicMapEmpty) {
                List<LearnerSubtopicPerformance> candidateSubtopics = subtopicRepository.findByCandidateId(candidateId);
                if (candidateSubtopics == null || candidateSubtopics.isEmpty()) {
                    candidateSubtopics = savedPerfs;
                }
                rollupTopicAccuracyFromSubtopics(profile, candidateSubtopics);
            }
        } catch (Exception e) {
            log.warn("Failed to update subtopic performance for candidate {}: {}", candidateId, e.getMessage());
        }
    }

    private void rollupTopicAccuracyFromSubtopics(LearnerProfile profile, List<LearnerSubtopicPerformance> subtopics) {
        if (subtopics == null || subtopics.isEmpty()) return;
        try {
            Map<String, List<LearnerSubtopicPerformance>> byTopic = new HashMap<>();
            for (LearnerSubtopicPerformance s : subtopics) {
                if (s.getTopicName() != null && !s.getTopicName().isBlank()) {
                    byTopic.computeIfAbsent(s.getTopicName(), k -> new ArrayList<>()).add(s);
                }
            }

            if (byTopic.isEmpty()) return;

            Map<String, Map<String, Object>> rolledUpMap = new HashMap<>();
            List<String> weakTopics = new ArrayList<>();
            List<String> strongTopics = new ArrayList<>();

            for (Map.Entry<String, List<LearnerSubtopicPerformance>> entry : byTopic.entrySet()) {
                String topic = entry.getKey();
                List<LearnerSubtopicPerformance> topicSubs = entry.getValue();
                int totalAttempts = topicSubs.stream().mapToInt(LearnerSubtopicPerformance::getTotalAttempts).sum();
                int totalCorrect = topicSubs.stream().mapToInt(LearnerSubtopicPerformance::getTotalCorrect).sum();

                double acc = totalAttempts > 0 ? (double) totalCorrect / totalAttempts : 0.0;
                double accPercent = totalAttempts > 0 ? ((double) totalCorrect * 100.0 / totalAttempts) : 0.0;

                Map<String, Object> topicData = new HashMap<>();
                topicData.put("accuracy", Math.round(acc * 1000.0) / 1000.0);
                topicData.put("attempts", totalAttempts);
                rolledUpMap.put(topic, topicData);

                if (accPercent < 60.0) {
                    weakTopics.add(topic);
                } else if (accPercent >= 75.0) {
                    strongTopics.add(topic);
                }
            }

            profile.setTopicAccuracyMap(objectMapper.writeValueAsString(rolledUpMap));
            profile.setWeakTopics(objectMapper.writeValueAsString(weakTopics));
            profile.setStrongTopics(objectMapper.writeValueAsString(strongTopics));
        } catch (Exception e) {
            log.warn("Failed to rollup topic accuracy from subtopics: {}", e.getMessage());
        }
    }

    @Transactional
    public LearnerProfile getOrCreateProfile(UUID candidateId) {
        return profileRepository.findByCandidateId(candidateId)
            .orElseGet(() -> profileRepository.save(LearnerProfile.builder()
                .candidateId(candidateId)
                .totalPracticeSessions(0)
                .totalQuestionsAttempted(0)
                .overallAccuracy(0.0)
                .topicAccuracyMap("{}")
                .subtopicAccuracyMap("{}")
                .weakTopics("[]")
                .strongTopics("[]")
                .build()));
    }

    @Transactional(readOnly = true)
    public LearnerProfileDto getProfile(UUID candidateId) {
        return profileRepository.findByCandidateId(candidateId)
            .map(p -> new LearnerProfileDto(p.getCandidateId(), p.getTotalPracticeSessions(),
                p.getOverallAccuracy(), p.getWeakTopics(), p.getStrongTopics(), p.getTopicAccuracyMap(),
                p.getSubtopicAccuracyMap() != null ? p.getSubtopicAccuracyMap() : "{}"))
            .orElse(new LearnerProfileDto(candidateId, 0, 0.0, "[]", "[]", "{}", "{}"));
    }
}
