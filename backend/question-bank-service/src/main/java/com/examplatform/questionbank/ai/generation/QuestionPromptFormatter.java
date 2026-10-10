/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 Open Digital Public Infrastructure (DPI) Platform Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 */
package com.examplatform.questionbank.ai.generation;

/**
 * Shared formatting utilities for constructing AI prompts and generation blueprints
 * from {@link QuestionGenerationRequest} payloads.
 *
 * @author Natassia Grid Development Team
 * @since 1.0.0
 */
public final class QuestionPromptFormatter {

    private QuestionPromptFormatter() {
        // Utility class
    }

    /**
     * Resolves the effective author description or prompt from the request,
     * preferring non-blank {@code rawTextInput} over {@code description}.
     *
     * @param request the question generation request
     * @return the resolved description string, or {@code null} if neither is provided
     */
    public static String resolveEffectiveDescription(QuestionGenerationRequest request) {
        if (request == null) {
            return null;
        }
        if (request.getRawTextInput() != null && !request.getRawTextInput().isBlank()) {
            return request.getRawTextInput();
        }
        return (request.getDescription() != null && !request.getDescription().isBlank())
                ? request.getDescription() : null;
    }

    /**
     * Appends structured question generation parameters to the provided {@link StringBuilder}.
     *
     * @param builder the target buffer to append into
     * @param request the question generation request containing parameters
     * @param prefix optional line prefix (e.g. "- " for bulleted prompts, or "" for blueprint headers)
     * @param descLabel the label for the description field (e.g. "Description/Requirements" or "Author Question Description/Prompt")
     * @param examLabel the label for the target exam field (e.g. "Target Exam" or "Target Exam Standard")
     */
    public static void appendGenerationParameters(
            StringBuilder builder,
            QuestionGenerationRequest request,
            String prefix,
            String descLabel,
            String examLabel) {

        String linePrefix = prefix != null ? prefix : "";
        builder.append(linePrefix).append("Subject: ").append(request.getSubject()).append("\n");
        builder.append(linePrefix).append("Topic: ").append(request.getTopic()).append("\n");

        if (request.getSubtopic() != null && !request.getSubtopic().isBlank()) {
            builder.append(linePrefix).append("Subtopic: ").append(request.getSubtopic()).append("\n");
        }

        String desc = resolveEffectiveDescription(request);
        if (desc != null && !desc.isBlank()) {
            builder.append(linePrefix).append(descLabel).append(": ").append(desc).append("\n");
        }

        builder.append(linePrefix).append("Difficulty: ").append(request.getDifficulty()).append("\n");
        builder.append(linePrefix).append("Cognitive Level: ").append(request.getCognitiveLevel()).append("\n");
        builder.append(linePrefix).append("Question Type: ").append(request.getQuestionType()).append("\n");

        if (request.getTargetExam() != null && !request.getTargetExam().isBlank()) {
            builder.append(linePrefix).append(examLabel).append(": ").append(request.getTargetExam()).append("\n");
        }
    }
}
