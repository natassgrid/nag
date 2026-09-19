/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 */

package com.examplatform.questionbank.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO representing a comprehension passage with its decrypted content and sub-questions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassageResponse {

    private UUID id;
    private String tenantId;
    private String title;
    private String content;
    private String contentFormat;
    private Long subjectId;
    private Long topicId;
    private String subject;
    private String topic;
    private boolean hasImages;
    private String state;
    private UUID authorId;
    private UUID reviewerId;
    private String encryptionKeyId;
    private List<QuestionResponse> subQuestions;
    private int subQuestionCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
