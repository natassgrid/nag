package com.examplatform.questionbank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubtopicRequest {

    @NotBlank(message = "Subtopic name is required")
    @Size(max = 200, message = "Subtopic name must not exceed 200 characters")
    private String name;

    private String description;
}
