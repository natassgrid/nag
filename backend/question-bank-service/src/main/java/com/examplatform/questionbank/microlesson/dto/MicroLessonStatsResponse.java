// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.dto;

public record MicroLessonStatsResponse(
        long total,
        long aiGenerated,
        long inReview,
        long approved,
        long published,
        long rejected
) {}
