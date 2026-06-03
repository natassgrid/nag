package com.examplatform.papergenerator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a gap between what a blueprint rule requires and what's available
 * in the question bank. Used when paper generation cannot be satisfied.
 *
 * Validates: Requirements 8.5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GapDetail {

    private String subject;

    private String topic;

    private String difficulty;

    private int needed;

    private int available;
}
