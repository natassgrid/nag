/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
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

package com.examplatform.examination.domain;

import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Seat allocation for a specific (shift, centre) pair.
 *
 * <p>Each entry records the breakdown of seats for one shift at one centre.
 * The service layer validates that {@link #availableSeats} never drops below
 * zero when candidates are allocated (Req 7b.6). A DB CHECK constraint
 * provides a backstop.
 *
 * Validates: Requirements 7b.6
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "shift_seat_allocation", schema = "examination_service")
public class ShiftSeatAllocation extends BaseEntity {

    /** FK → exam_shift.id */
    @Column(name = "shift_id", nullable = false, columnDefinition = "uuid")
    private UUID shiftId;

    /** FK → examination_centre.id */
    @Column(name = "centre_id", nullable = false, columnDefinition = "uuid")
    private UUID centreId;

    /** Total seats configured for this shift at this centre. */
    @Column(name = "total_seats", nullable = false)
    @Builder.Default
    private int totalSeats = 0;

    /** Currently available (unallocated) seats. Must remain ≥ 0. */
    @Column(name = "available_seats", nullable = false)
    @Builder.Default
    private int availableSeats = 0;

    /** Seats held in reserve (not yet released to general allocation). */
    @Column(name = "reserved_seats", nullable = false)
    @Builder.Default
    private int reservedSeats = 0;

    /** Seats reserved for Persons with Disabilities (PwD). */
    @Column(name = "pwd_seats", nullable = false)
    @Builder.Default
    private int pwdSeats = 0;

    /** Emergency buffer seats held back for operational contingencies. */
    @Column(name = "emergency_buffer_seats", nullable = false)
    @Builder.Default
    private int emergencyBufferSeats = 0;

    /** Seats reserved for female candidates (if applicable for the examination). */
    @Column(name = "female_reserved_seats", nullable = false)
    @Builder.Default
    private int femaleReservedSeats = 0;

    /** Seats reserved for special categories (SC/ST/OBC/EWS etc.). */
    @Column(name = "special_category_seats", nullable = false)
    @Builder.Default
    private int specialCategorySeats = 0;
}
