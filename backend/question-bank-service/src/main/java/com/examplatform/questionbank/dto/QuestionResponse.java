package com.examplatform.questionbank.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO representing a question with decrypted content fields.
 *
 * Validates: Requirements 4.1, 4.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {

    private UUID id;
    private String subject;
    private String topic;
    private String subtopic;
    private String chapter;
    private String difficulty;
    private String cognitiveLevel;
    private String questionType;
    private String content;
    private String answerKey;
    private String state;
    private UUID authorId;
    private LocalDateTime createdAt;

    /** Parsed options for MCQ/MSQ questions */
    private java.util.List<QuestionOption> options;
}
