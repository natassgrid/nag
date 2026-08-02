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
 * Request DTO for amending a Published examination schedule.
 * Change reason is mandatory per Req 7b.8.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmendScheduleRequest {

    @NotBlank(message = "changeReason is mandatory when amending a published schedule")
    @Size(max = 1000)
    private String changeReason;

    @NotBlank
    @Size(max = 255)
    private String scheduleName;

    @Size(max = 100)
    private String notificationNumber;

    @NotNull
    private LocalDate examDate;

    private LocalDate reserveDate;

    private LocalDate effectiveFrom;

    @Builder.Default
    private String timeZone = "Asia/Kolkata";
}
