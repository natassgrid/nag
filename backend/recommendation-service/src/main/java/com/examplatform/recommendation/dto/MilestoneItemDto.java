// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.dto;

import com.examplatform.recommendation.domain.PerformanceBracket;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MilestoneItemDto(
        String id,
        Long subtopicId,
        String subtopicName,
        String topicName,
        String subjectName,
        PerformanceBracket bracket,
        String actionType,
        String title,
        String description,
        Integer estimatedMinutes,
        boolean completed,
        Instant completedAt,
        UUID microLessonId
) implements Serializable {

    public MilestoneItemDto(
            String id,
            String subtopicName,
            String topicName,
            String subjectName,
            PerformanceBracket bracket,
            String actionType,
            String title,
            String description,
            Integer estimatedMinutes,
            boolean completed) {
        this(
                id,
                null,
                subtopicName,
                topicName,
                subjectName,
                bracket,
                actionType,
                title,
                description,
                estimatedMinutes,
                completed,
                completed ? Instant.now() : null,
                null
        );
    }
}
