/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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

import com.examplatform.shared.dto.SubtopicPerformanceDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Granular subject, topic, and subtopic score and accuracy breakdown DTO for examination results.
 * Supports interoperability with shared-lib {@link SubtopicPerformanceDto} for learner mastery updates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaxonomyBreakdownDto implements Serializable {

    private Long subjectId;
    private String subject;
    private String subjectName;
    private Long topicId;
    private String topic;
    private String topicName;
    private Long subtopicId;
    private String subtopic;
    private String subtopicName;
    private double score;
    private double maxMarks;
    private double accuracyRate;
    private double accuracyPercent;
    private int questionCount;
    private int totalQuestions;
    private int attemptedCount;
    private int correctCount;
    private int incorrectCount;
    private int skippedCount;
    private long totalTimeSpentMs;
    private long avgTimeSpentMs;
    private long benchmarkTimeMs;
    private double skipRatePercent;
    private double paceRatio;

    /**
     * Resolves display name aliases and synchronized counts defensively.
     */
    public TaxonomyBreakdownDto normalize() {
        if (subjectName == null && subject != null) {
            subjectName = subject;
        } else if (subject == null && subjectName != null) {
            subject = subjectName;
        }

        if (topicName == null && topic != null) {
            topicName = topic;
        } else if (topic == null && topicName != null) {
            topic = topicName;
        }

        if (subtopicName == null && subtopic != null) {
            subtopicName = subtopic;
        } else if (subtopic == null && subtopicName != null) {
            subtopic = subtopicName;
        }

        if (totalQuestions <= 0 && questionCount > 0) {
            totalQuestions = questionCount;
        } else if (questionCount <= 0 && totalQuestions > 0) {
            questionCount = totalQuestions;
        }

        if (accuracyPercent <= 0 && accuracyRate > 0) {
            accuracyPercent = accuracyRate;
        } else if (accuracyRate <= 0 && accuracyPercent > 0) {
            accuracyRate = accuracyPercent;
        }

        if (incorrectCount <= 0 && attemptedCount > correctCount) {
            incorrectCount = attemptedCount - correctCount;
        }

        if (skippedCount <= 0 && totalQuestions > attemptedCount) {
            skippedCount = totalQuestions - attemptedCount;
        }

        if (avgTimeSpentMs <= 0 && attemptedCount > 0 && totalTimeSpentMs > 0) {
            avgTimeSpentMs = totalTimeSpentMs / attemptedCount;
        }

        return this;
    }

    /**
     * Converts to shared-lib {@link SubtopicPerformanceDto} for learner mastery engine consumption.
     */
    public SubtopicPerformanceDto toSubtopicPerformanceDto() {
        normalize();
        return new SubtopicPerformanceDto(
                subjectId,
                subjectName != null ? subjectName : subject,
                topicId,
                topicName != null ? topicName : topic,
                subtopicId,
                subtopicName != null ? subtopicName : subtopic,
                totalQuestions > 0 ? totalQuestions : questionCount,
                attemptedCount,
                correctCount,
                incorrectCount,
                skippedCount,
                totalTimeSpentMs,
                avgTimeSpentMs,
                benchmarkTimeMs > 0 ? benchmarkTimeMs : 90000L,
                accuracyPercent > 0 ? accuracyPercent : accuracyRate,
                skipRatePercent,
                paceRatio > 0 ? paceRatio : 1.0
        );
    }
}
