package com.examplatform.examination.dto;

import com.examplatform.examination.domain.Section;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for examination configuration data.
 *
 * Validates: Requirements 7.1, 7.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExaminationResponse {

    private UUID id;
    private String name;
    private int durationMinutes;
    private int totalMarks;
    private boolean negativeMarkingEnabled;
    private double negativeMarkingValue;
    private String navigationPolicy;
    private String calculatorPolicy;
    private boolean reviewFlagEnabled;
    private List<Section> sections;
    private String status;
    private LocalDateTime createdAt;
}
