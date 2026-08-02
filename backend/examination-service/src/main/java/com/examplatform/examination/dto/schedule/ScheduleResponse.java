package com.examplatform.examination.dto.schedule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for an examination schedule.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleResponse {

    private UUID id;
    private UUID examinationId;
    private String scheduleName;
    private int scheduleVersion;
    private String notificationNumber;
    private LocalDate examDate;
    private LocalDate reserveDate;
    private String timeZone;
    private String status;
    private String changeReason;
    private LocalDate effectiveFrom;
    private UUID previousVersionId;
    private UUID createdBy;
    private UUID modifiedBy;
    private UUID approvedBy;
    private Instant approvedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
