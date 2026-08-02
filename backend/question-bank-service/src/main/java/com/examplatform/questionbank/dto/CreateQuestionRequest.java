package com.examplatform.questionbank.dto;

import com.examplatform.questionbank.domain.enums.CognitiveLevel;
import com.examplatform.questionbank.domain.enums.DifficultyLevel;
import com.examplatform.questionbank.domain.enums.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for creating a new question.
 * Supports rich content types: HTML5, SVG, LaTeX, MathML, or references to media files.
 *
 * Validates: Requirements 4.1, 4.2, 4.3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuestionRequest {

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Topic is required")
    private String topic;

    private String subtopic;

    private String chapter;

    @NotNull(message = "Difficulty level is required")
    private DifficultyLevel difficulty;

    @NotNull(message = "Cognitive level is required")
    private CognitiveLevel cognitiveLevel;

    @NotNull(message = "Question type is required")
    private QuestionType questionType;

    @NotBlank(message = "Content is required")
    private String content;

    private String answerKey;

    private String contentType;

    /** Options for MCQ/MSQ questions (2-6 items, A-F) */
    private java.util.List<QuestionOption> options;
}
