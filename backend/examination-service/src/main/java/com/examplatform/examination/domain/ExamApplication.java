/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
 */

package com.examplatform.examination.domain;

import com.examplatform.shared.constants.SharedConstants;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks a candidate's application to an examination with centre & shift preferences.
 *
 * One candidate can apply to many exams; one exam can have many candidates.
 * Unique constraint on (candidateId, examinationId, tenantId) prevents double-apply.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "exam_application", schema = "examination_service")
public class ExamApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "candidate_id", nullable = false, columnDefinition = "uuid")
    private UUID candidateId;

    @Column(name = "examination_id", nullable = false, columnDefinition = "uuid")
    private UUID examinationId;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    /** APPLIED | CONFIRMED | REJECTED */
    @Column(name = SharedConstants.STATUS, nullable = false, length = 20)
    private String status;

    @Column(name = "hall_ticket_number", length = 30)
    private String hallTicketNumber;

    @Column(name = "first_choice_centre_id", columnDefinition = "uuid")
    private UUID firstChoiceCentreId;

    @Column(name = "second_choice_centre_id", columnDefinition = "uuid")
    private UUID secondChoiceCentreId;

    @Column(name = "third_choice_centre_id", columnDefinition = "uuid")
    private UUID thirdChoiceCentreId;

    @Column(name = "allocated_centre_id", columnDefinition = "uuid")
    private UUID allocatedCentreId;

    @Column(name = "allocated_shift_id", columnDefinition = "uuid")
    private UUID allocatedShiftId;

    @Column(name = "preferred_shift_id", columnDefinition = "uuid")
    private UUID preferredShiftId;

    @Column(name = "pwd_required")
    private Boolean pwdRequired;

    @Column(name = "scribe_required")
    private Boolean scribeRequired;

    @Column(name = "qr_verification_hash", length = 128)
    private String qrVerificationHash;

    @CreationTimestamp
    @Column(name = "applied_at", updatable = false)
    private LocalDateTime appliedAt;
}
