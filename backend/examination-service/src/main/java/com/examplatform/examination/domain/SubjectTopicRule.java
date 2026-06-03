package com.examplatform.examination.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Topic-level selection rule within a Section.
 * NOT a JPA entity — serialized as part of the Section JSONB structure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectTopicRule {
    private String topic;
    private String difficulty;       // EASY, MEDIUM, HARD or null (any)
    private String cognitiveLevel;   // or null (any)
    private int questionCount;
}
