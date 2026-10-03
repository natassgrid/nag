// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.service;

import com.examplatform.recommendation.domain.LearnerProfile;
import com.examplatform.recommendation.domain.Recommendation;
import com.examplatform.recommendation.repository.RecommendationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecommendationAiService {
    private static final Logger log = LoggerFactory.getLogger(RecommendationAiService.class);
    
    private final ChatClient chatClient;
    private final RecommendationRepository recommendationRepository;
    private final ObjectMapper objectMapper;
    
    private static final String SYSTEM_PROMPT = """
        You are an expert educational counsellor specializing in Indian competitive examination preparation.
        Analyze the student's practice performance data and generate personalized study recommendations.
        Respond ONLY with a valid JSON object matching this exact schema:
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
        
        try {
            String userPrompt = buildPrompt(profile);
            String response = chatClient
                .prompt()
                .system(SYSTEM_PROMPT)
                .user(userPrompt)
                .call()
                .content();
            
            // Parse response
            Map<String, Object> parsed = objectMapper.readValue(response, new TypeReference<>() {});
            
            rec.setWeakTopicRecommendations(objectMapper.writeValueAsString(parsed.get("weakTopicRecommendations")));
            rec.setStudyPlanItems(objectMapper.writeValueAsString(parsed.get("studyPlanItems")));
            rec.setMotivationalMessage((String) parsed.get("motivationalMessage"));
            rec.setStatus("GENERATED");
            rec.setGeneratedAt(Instant.now());
            rec.setModelUsed("configured-model");
        } catch (Exception e) {
            log.warn("AI recommendation generation failed for candidate {}: {}", profile.getCandidateId(), e.getMessage());
            rec.setStatus("PENDING"); // stays pending for retry
        }
        recommendationRepository.save(rec);
    }
    
    private String buildPrompt(LearnerProfile profile) {
        return String.format("""
            Student Performance Summary:
            - Total practice sessions completed: %d
            - Overall accuracy: %.1f%%
            - Weak topics (accuracy < 60%%): %s
            - Strong topics: %s
            - Topic accuracy breakdown: %s
            
            Generate personalized study recommendations to help this student improve.
            Focus recommendations on the weak topics.
            Create a realistic 7-day study plan.
            Keep the motivational message encouraging and specific to their performance level.
            """,
            profile.getTotalPracticeSessions(),
            profile.getOverallAccuracy() * 100,
            profile.getWeakTopics() != null ? profile.getWeakTopics() : "[]",
            profile.getStrongTopics() != null ? profile.getStrongTopics() : "[]",
            profile.getTopicAccuracyMap() != null ? profile.getTopicAccuracyMap() : "{}"
        );
    }
}
