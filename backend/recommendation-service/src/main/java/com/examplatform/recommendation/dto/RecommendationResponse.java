// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record RecommendationResponse(
        UUID id,
        UUID candidateId,
        UUID triggerSessionId,
        String status,
        Instant generatedAt,
        List<WeakTopicItemDto> weakTopicRecommendations,
        List<StudyPlanDay> studyPlan,
        List<String> suggestedPracticeSetIds,
        String motivationalMessage
) implements Serializable {

    @JsonProperty("studyPlanItems")
    public List<StudyPlanDay> getStudyPlanItems() {
        return studyPlan;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StudyPlanDay(
            int day,
            @JsonAlias({"topicName", "topic"})
            String focusTopic,
            List<String> tasks,
            @JsonAlias({"estimatedMinutes", "duration"})
            int durationMinutes,
            Boolean completed
    ) implements Serializable {

        public StudyPlanDay(int day, String focusTopic, List<String> tasks, int durationMinutes) {
            this(day, focusTopic, tasks, durationMinutes, false);
        }

        @JsonIgnore
        public String getTopicName() {
            return focusTopic;
        }

        @JsonIgnore
        public int getEstimatedMinutes() {
            return durationMinutes;
        }

        @JsonIgnore
        public Boolean getCompleted() {
            return completed != null && completed;
        }
    }
}
