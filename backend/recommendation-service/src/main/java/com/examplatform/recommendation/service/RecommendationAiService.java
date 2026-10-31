// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.service;

import com.examplatform.recommendation.domain.LearnerProfile;
import com.examplatform.recommendation.domain.LearnerSubtopicPerformance;
import com.examplatform.recommendation.domain.Recommendation;
import com.examplatform.recommendation.dto.RecommendationResponse;
import com.examplatform.recommendation.dto.WeakTopicItemDto;
import com.examplatform.recommendation.repository.LearnerSubtopicPerformanceRepository;
import com.examplatform.recommendation.repository.RecommendationRepository;
import com.examplatform.shared.constants.SharedConstants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RecommendationAiService {
    private static final Logger log = LoggerFactory.getLogger(RecommendationAiService.class);
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```");

    private final ChatClient chatClient;
    private final RecommendationRepository recommendationRepository;
    private final ObjectMapper objectMapper;
    private final LearnerSubtopicPerformanceRepository subtopicPerformanceRepository;

    @Autowired
    public RecommendationAiService(
            @Autowired(required = false) ChatClient chatClient,
            RecommendationRepository recommendationRepository,
            ObjectMapper objectMapper,
            @Autowired(required = false) LearnerSubtopicPerformanceRepository subtopicPerformanceRepository) {
        this.chatClient = chatClient;
        this.recommendationRepository = recommendationRepository;
        this.objectMapper = objectMapper;
        this.subtopicPerformanceRepository = subtopicPerformanceRepository;
    }

    public RecommendationAiService(
            ChatClient chatClient,
            RecommendationRepository recommendationRepository,
            ObjectMapper objectMapper) {
        this(chatClient, recommendationRepository, objectMapper, null);
    }

    private static final String SYSTEM_PROMPT = """
        You are an expert educational counsellor specializing in Indian competitive examination preparation.
        Analyze the student's practice performance data and generate personalized study recommendations.

        CRITICAL INSTRUCTION:
        Respond strictly with a raw valid JSON object only.
        Do NOT wrap in markdown code blocks or fences (no ``` or ```json).
        Do NOT include conversational preambles, introductory filler, or commentary outside the JSON object.

        The output must match this exact schema:
        {
          "weakTopicRecommendations": [
            {"topicName": "string", "currentAccuracy": 0.0, "subtopicsToRevise": ["string"]}
          ],
          "studyPlanItems": [
            {"day": 1, "topicName": "string", "estimatedMinutes": 45}
          ],
          "motivationalMessage": "string (max 2 sentences)"
        }
        """;

    @Async("aiTaskExecutor")
    public void generateAndSave(LearnerProfile profile, UUID triggerSessionId) {
        // Create a PENDING recommendation first
        Recommendation rec = Recommendation.builder()
            .candidateId(profile.getCandidateId())
            .triggerSessionId(triggerSessionId)
            .status("PENDING")
            .build();
        recommendationRepository.save(rec);

        boolean generatedByAi = tryGenerateWithAi(rec, profile, "background");
        if (!generatedByAi) {
            populateDeterministicFallback(rec, profile, null);
        }

        recommendationRepository.save(rec);
    }

    public Recommendation generateOnDemand(LearnerProfile profile, String examTarget) {
        Recommendation rec = Recommendation.builder()
                .candidateId(profile.getCandidateId())
                .status("PENDING")
                .build();
        recommendationRepository.save(rec);

        boolean generatedByAi = tryGenerateWithAi(rec, profile, "on-demand");
        if (!generatedByAi) {
            populateDeterministicFallback(rec, profile, examTarget);
        }

        return recommendationRepository.save(rec);
    }

    private boolean tryGenerateWithAi(Recommendation rec, LearnerProfile profile, String contextDesc) {
        if (chatClient == null) {
            return false;
        }
        try {
            String userPrompt = buildPrompt(profile);
            String response = chatClient
                    .prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .content();

            String jsonText = sanitizeJson(response);
            if (!jsonText.isBlank()) {
                Map<String, Object> parsed = objectMapper.readValue(jsonText, new TypeReference<>() {});
                Object weakObj = parsed.get("weakTopicRecommendations");
                Object planObj = parsed.get("studyPlanItems");
                String msg = (String) parsed.get("motivationalMessage");

                if (weakObj instanceof List<?> wl && !wl.isEmpty() && planObj instanceof List<?> pl && !pl.isEmpty()) {
                    rec.setWeakTopicRecommendations(objectMapper.writeValueAsString(wl));
                    rec.setStudyPlanItems(objectMapper.writeValueAsString(pl));
                    rec.setMotivationalMessage(msg != null ? msg : "");
                    rec.setStatus("GENERATED");
                    rec.setGeneratedAt(Instant.now());
                    rec.setModelUsed("configured-model");
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("AI recommendation generation {} failed for candidate {}, falling back to deterministic synthesis: {}",
                    contextDesc, profile.getCandidateId(), e.getMessage());
        }
        return false;
    }

    public Recommendation buildDeterministicRecommendation(LearnerProfile profile, UUID sessionId, String examTarget) {
        Recommendation rec = Recommendation.builder()
                .candidateId(profile.getCandidateId())
                .triggerSessionId(sessionId)
                .status("GENERATED")
                .build();
        populateDeterministicFallback(rec, profile, examTarget);
        return recommendationRepository.save(rec);
    }

    public void populateDeterministicFallback(Recommendation rec, LearnerProfile profile, String examTarget) {
        List<LearnerSubtopicPerformance> subtopics = subtopicPerformanceRepository != null
                ? subtopicPerformanceRepository.findByCandidateId(profile.getCandidateId())
                : List.of();

        List<WeakTopicItemDto> weakTopics = new ArrayList<>();

        if (subtopics != null && !subtopics.isEmpty()) {
            Map<String, List<LearnerSubtopicPerformance>> topicGroups = new LinkedHashMap<>();
            for (LearnerSubtopicPerformance p : subtopics) {
                if (p.getAvgAccuracyPercent() < 75.0 && p.getTopicName() != null && !p.getTopicName().isBlank()) {
                    topicGroups.computeIfAbsent(p.getTopicName(), k -> new ArrayList<>()).add(p);
                }
            }

            for (Map.Entry<String, List<LearnerSubtopicPerformance>> entry : topicGroups.entrySet()) {
                String topicName = entry.getKey();
                List<LearnerSubtopicPerformance> perfs = entry.getValue();
                double avgAcc = perfs.stream().mapToDouble(LearnerSubtopicPerformance::getAvgAccuracyPercent).average().orElse(0.0);
                double normalizedAcc = Math.round(avgAcc) / 100.0;
                List<String> subtopicsToRevise = perfs.stream()
                        .map(LearnerSubtopicPerformance::getSubtopicName)
                        .filter(s -> s != null && !s.isBlank())
                        .distinct()
                        .toList();
                String subjectName = perfs.stream()
                        .map(LearnerSubtopicPerformance::getSubjectName)
                        .filter(s -> s != null && !s.isBlank())
                        .findFirst()
                        .orElse(SharedConstants.GENERAL);
                Long subtopicId = perfs.stream()
                        .map(LearnerSubtopicPerformance::getSubtopicId)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);
                weakTopics.add(new WeakTopicItemDto(topicName, normalizedAcc, subtopicsToRevise, subjectName, subtopicId));
            }
        }

        if (weakTopics.isEmpty() && profile.getWeakTopics() != null && !profile.getWeakTopics().isBlank()) {
            try {
                List<String> profileWeak = objectMapper.readValue(profile.getWeakTopics(), new TypeReference<List<String>>() {});
                Map<String, Map<String, Object>> topicMap = new HashMap<>();
                if (profile.getTopicAccuracyMap() != null && !profile.getTopicAccuracyMap().isBlank()) {
                    topicMap = objectMapper.readValue(profile.getTopicAccuracyMap(), new TypeReference<>() {});
                }
                for (String topic : profileWeak) {
                    double acc = 0.45;
                    if (topicMap.containsKey(topic)) {
                        Object accObj = topicMap.get(topic).get("accuracy");
                        if (accObj instanceof Number num) {
                            double val = num.doubleValue();
                            acc = val > 1.0 ? Math.round(val) / 100.0 : Math.round(val * 100.0) / 100.0;
                        }
                    }
                    weakTopics.add(new WeakTopicItemDto(
                            topic,
                            acc,
                            List.of(topic + " Core Principles"),
                        SharedConstants.GENERAL,
                            null
                    ));
                }
            } catch (Exception e) {
                log.warn("Failed to parse weak topics from profile: {}", e.getMessage());
            }
        }

        if (weakTopics.isEmpty()) {
            String effectiveExam = (examTarget != null && !examTarget.isBlank()) ? examTarget.toUpperCase() :
                SharedConstants.JEE_MAIN;
            if (effectiveExam.contains("IBPS") || effectiveExam.contains("SSC") || effectiveExam.contains("BANK") || effectiveExam.contains("CGL")) {
                weakTopics.add(new WeakTopicItemDto("Quantitative Aptitude", 0.50, List.of("Percentage & Ratios", "Data Interpretation", "Speed Math & Simplification"), "Quantitative Aptitude", 101L));
                weakTopics.add(new WeakTopicItemDto("Reasoning Ability", 0.50, List.of("Syllogism", "Seating Arrangement", "Puzzles & Inequalities"), "Reasoning", 102L));
                weakTopics.add(new WeakTopicItemDto("English Comprehension", 0.50, List.of("Reading Comprehension", "Error Spotting", "Cloze Test"), "English", 103L));
            } else if (effectiveExam.contains("NEET")) {
                weakTopics.add(new WeakTopicItemDto("Biology", 0.50, List.of("Genetics & Evolution", "Human Physiology", "Ecology & Diversity"), "Biology", 201L));
                weakTopics.add(new WeakTopicItemDto("Organic Chemistry", 0.50, List.of("Carbon Compounds", "Reaction Mechanisms", "Biomolecules"), "Chemistry", 202L));
                weakTopics.add(new WeakTopicItemDto("Physics", 0.50, List.of("Optics", "Electrostatics & Current", "Thermodynamics"), "Physics", 203L));
            } else {
                weakTopics.add(new WeakTopicItemDto("Mathematics", 0.50, List.of("Calculus & Limits", "Coordinate Geometry", "Algebra & Matrices"), "Mathematics", 301L));
                weakTopics.add(new WeakTopicItemDto("Physics", 0.50, List.of("Rotational Mechanics", "Electromagnetism", "Modern Physics"), "Physics", 302L));
                weakTopics.add(new WeakTopicItemDto("Chemistry", 0.50, List.of("Chemical Equilibrium", "Organic Reaction Mechanisms", "Coordination Compounds"), "Chemistry", 303L));
            }
        }

        List<RecommendationResponse.StudyPlanDay> studyPlan = new ArrayList<>();
        for (int day = 1; day <= 5; day++) {
            WeakTopicItemDto currentTopic = weakTopics.get((day - 1) % weakTopics.size());
            String subtopicOrTopic = (currentTopic.subtopicsToRevise() != null && !currentTopic.subtopicsToRevise().isEmpty())
                    ? currentTopic.subtopicsToRevise().get((day - 1) % currentTopic.subtopicsToRevise().size())
                    : currentTopic.topicName();
            List<String> tasks = List.of(
                    "Study Concept Micro-Lesson & Formula Cheat Sheet for " + subtopicOrTopic,
                    "Solve 15 targeted practice questions focusing on accuracy and trap avoidance",
                    "Review step-by-step error log and mark milestone studied"
            );
            int duration = 45 + ((day % 2) * 15);
            studyPlan.add(new RecommendationResponse.StudyPlanDay(day, currentTopic.topicName(), tasks, duration, false));
        }
        studyPlan.add(new RecommendationResponse.StudyPlanDay(
                6,
                "Phase 2 Adaptive Application & Speed Drill",
                List.of("Attempt 15-question timed Adaptive Quiz (>= 70% target)", "Analyze pace ratio on speed-bottleneck subtopics"),
                60,
                false
        ));
        studyPlan.add(new RecommendationResponse.StudyPlanDay(
                7,
                "Full Revision & Phase 3 Gatekeeper Verification",
                List.of("Revise Formula Cheat Sheets for all weak topics", "Attempt 30-question Gatekeeper Assessment (>= 75% target)"),
                60,
                false
        ));

        String effectiveExam = (examTarget != null && !examTarget.isBlank()) ? examTarget : "your target exam";
        String motivationalMessage;
        double accuracyPercent = profile.getOverallAccuracy() <= 1.0 ? profile.getOverallAccuracy() * 100.0 : profile.getOverallAccuracy();
        if (profile.getTotalPracticeSessions() == 0) {
            motivationalMessage = "Welcome to your AI Study Coach! We have prepared a 7-Day Foundation Study Plan for "
                    + effectiveExam + ". Take your first baseline practice test to unlock subtopic-level telemetry and personalized remediation.";
        } else if (accuracyPercent >= 75.0) {
            motivationalMessage = String.format("Outstanding performance! Your overall accuracy across %d session(s) is %.1f%%. Follow this 7-day mastery plan to consolidate your speed and clear the Gatekeeper Assessment.",
                    profile.getTotalPracticeSessions(), accuracyPercent);
        } else {
            motivationalMessage = String.format("Based on %d practice session(s) (overall accuracy: %.1f%%), we identified %d priority focus area(s). Complete the Phase 1 Micro-Lessons and follow your 7-Day Study Plan below to boost your accuracy above 75%%!",
                    profile.getTotalPracticeSessions(), accuracyPercent, weakTopics.size());
        }

        try {
            rec.setWeakTopicRecommendations(objectMapper.writeValueAsString(weakTopics));
            rec.setStudyPlanItems(objectMapper.writeValueAsString(studyPlan));
        } catch (Exception e) {
            log.warn("Failed to serialize deterministic fallback: {}", e.getMessage());
            rec.setWeakTopicRecommendations("[]");
            rec.setStudyPlanItems("[]");
        }
        rec.setMotivationalMessage(motivationalMessage);
        rec.setStatus("GENERATED");
        rec.setModelUsed("deterministic-coach-v1");
        rec.setGeneratedAt(Instant.now());
    }

    public String sanitizeJson(String response) {
        if (response == null || response.isBlank()) return "";
        String text = response.trim();
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(text);
        if (matcher.find()) {
            text = matcher.group(1).trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private String buildPrompt(LearnerProfile profile) {
        StringBuilder subtopicsSummary = new StringBuilder();
        if (subtopicPerformanceRepository != null) {
            List<LearnerSubtopicPerformance> perfs = subtopicPerformanceRepository.findByCandidateId(profile.getCandidateId());
            if (perfs != null && !perfs.isEmpty()) {
                subtopicsSummary.append("\nSubtopic-level Performance:\n");
                for (LearnerSubtopicPerformance p : perfs) {
                    subtopicsSummary.append(String.format("- %s (%s): %.1f%% accuracy, pace: %.2fx, skipped: %.1f%%\n",
                            p.getSubtopicName(), p.getTopicName(), p.getAvgAccuracyPercent(), p.getAvgPaceRatio(), p.getAvgSkipRatePercent()));
                }
            }
        }

        double accuracyPercent = profile.getOverallAccuracy() <= 1.0 ? profile.getOverallAccuracy() * 100.0 : profile.getOverallAccuracy();

        return String.format("""
            Student Performance Summary:
            - Total practice sessions completed: %d
            - Overall accuracy: %.1f%%
            - Weak topics (accuracy < 60%%): %s
            - Strong topics: %s
            - Topic accuracy breakdown: %s
            %s
            Generate personalized study recommendations to help this student improve.
            Focus recommendations on the weak topics.
            Create a realistic 7-day study plan.
            Keep the motivational message encouraging and specific to their performance level.
            Return ONLY the raw JSON object.
            """,
            profile.getTotalPracticeSessions(),
            accuracyPercent,
            profile.getWeakTopics() != null ? profile.getWeakTopics() : "[]",
            profile.getStrongTopics() != null ? profile.getStrongTopics() : "[]",
            profile.getTopicAccuracyMap() != null ? profile.getTopicAccuracyMap() : "{}",
            subtopicsSummary.toString()
        );
    }
}
