package com.examplatform.examination.dto;

import com.examplatform.examination.domain.Section;
import com.examplatform.examination.domain.enums.CalculatorPolicy;
import com.examplatform.examination.domain.enums.NavigationPolicy;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating or updating an examination configuration.
 *
 * Validates: Requirements 7.1, 7.2, 7.3, 7.4, 7.5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExaminationRequest {

    @NotBlank
    private String name;

    @Min(1)
    private int durationMinutes;

    @Min(1)
    private int totalMarks;

    private boolean negativeMarkingEnabled;

    private double negativeMarkingValue;

    @NotNull
    private NavigationPolicy navigationPolicy;

    @NotNull
    private CalculatorPolicy calculatorPolicy;

    private boolean reviewFlagEnabled;

    @NotEmpty
    private List<Section> sections;
}
