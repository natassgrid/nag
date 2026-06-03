package com.examplatform.delivery.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request payload for question navigation within an exam session.
 * Specifies the target question/section the candidate wants to navigate to.
 *
 * Validates: Requirements 9.2, 9.5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NavigationRequest {

    @NotNull
    private UUID sessionId;

    /**
     * Target question index within the exam paper (0-based).
     */
    private Integer targetQuestionIndex;

    /**
     * Target section index for section-based navigation (0-based).
     * Used with RESTRICTED and Section_Mode rendering.
     */
    private Integer targetSectionIndex;
}
