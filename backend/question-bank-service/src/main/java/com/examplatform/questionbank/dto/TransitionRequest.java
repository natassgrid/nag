package com.examplatform.questionbank.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for transitioning a question through the lifecycle FSM.
 *
 * Validates: Requirements 4.6, 5.5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransitionRequest {

    @NotBlank(message = "Target state must not be blank")
    private String targetState;

    private String comments;
}
