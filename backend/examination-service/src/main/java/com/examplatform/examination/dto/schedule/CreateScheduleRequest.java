package com.examplatform.examination.dto.schedule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for creating a new examination schedule.
 * Validates: Requirements 7b.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateScheduleRequest {

    @NotBlank
    @Size(max = 255)
    private String scheduleName;

    @Size(max = 100)
    private String notificationNumber;

    @NotNull
    private LocalDate examDate;

    private LocalDate reserveDate;

    @Builder.Default
    private String timeZone = "Asia/Kolkata";
}
