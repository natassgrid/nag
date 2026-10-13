/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.delivery.domain;

import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an active exam session for a candidate.
 * Tracks the live state of an ongoing examination delivery.
 */
@Entity
@Table(name = "exam_session", schema = "delivery_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamSession extends BaseEntity {

    @Column(name = "session_id", nullable = false, unique = true)
    private UUID sessionId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "exam_id", nullable = false)
    private UUID examId;

    @Column(name = "shift_id", nullable = false)
    private UUID shiftId;

    @Column(name = "paper_id", nullable = false)
    private UUID paperId;

    @Enumerated(EnumType.STRING)
    @Column(name = SharedConstants.STATUS, nullable = false, length = 20)
    private ExamSessionStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "scheduled_end_at", nullable = false)
    private Instant scheduledEndAt;

    @Column(name = "current_question_index", nullable = false)
    private Integer currentQuestionIndex;

    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode;

    @Column(name = "full_screen_exit_count", nullable = false)
    private Integer fullScreenExitCount;

    /**
     * Exam session lifecycle states.
     */
    public enum ExamSessionStatus {
        ACTIVE,
        SUBMITTED,
        EXPIRED
    }
}
