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

package com.examplatform.shared.enrollment;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.UUID;

/**
 * Immutable reference linking a Candidate to their active Examination enrollment.
 *
 * <p>Under the Unified Exam Spine architecture, {@code learner_enrollment} is the
 * canonical anchor binding a learner to their targeted examination. Downstream services
 * (diagnostic testing, adaptive learning paths, practice sets, mocks, evaluation, and analytics)
 * pass this reference to scope syllabus, mastery state, and performance metrics.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EnrollmentRef(
        UUID candidateId,
        UUID enrollmentId,
        UUID examId
) implements Serializable {

    public static EnrollmentRef of(UUID candidateId, UUID enrollmentId, UUID examId) {
        return new EnrollmentRef(candidateId, enrollmentId, examId);
    }

    public static EnrollmentRef of(String candidateId, String enrollmentId, String examId) {
        return new EnrollmentRef(
                candidateId != null ? UUID.fromString(candidateId) : null,
                enrollmentId != null ? UUID.fromString(enrollmentId) : null,
                examId != null ? UUID.fromString(examId) : null
        );
    }
}
