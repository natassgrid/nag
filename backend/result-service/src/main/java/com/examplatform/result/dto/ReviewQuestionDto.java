/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.result.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Represents a single question with candidate's response for the post-exam review.
 *
 * Validates: SPEC-UI3
 */
@Data
@Builder
public class ReviewQuestionDto {
    private UUID questionId;
    private int questionNumber;
    private String content;
    private String subject;
    private String topic;
    private String difficulty;
    private String bloomsLevel;
    private List<ReviewOptionDto> options;
    private List<String> candidateSelectedOptionIds;
    private boolean isCorrect;
    private double marksAwarded;
    private long timeSpentMs;
    private double peerAccuracyPct;
    private String explanation;
}
