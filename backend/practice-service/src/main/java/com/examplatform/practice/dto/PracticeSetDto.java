// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PracticeSetDto(
        UUID id,
        String name,
        String description,
        String source,
        int durationMinutes,
        String subjectSlug,
        boolean published,
        int totalQuestions,
        Long subjectId,
        Long topicId,
        Long subtopicId
) {
    public PracticeSetDto(
            UUID id,
            String name,
            String description,
            String source,
            int durationMinutes,
            String subjectSlug,
            boolean published,
            int totalQuestions
    ) {
        this(id, name, description, source, durationMinutes, subjectSlug, published, totalQuestions, null, null, null);
    }
}
