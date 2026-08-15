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

package com.examplatform.delivery.client;

import com.examplatform.delivery.dto.ShiftAssignment;

import java.util.UUID;

/**
 * Client interface for retrieving shift assignment details from the examination-service.
 * Implementations may use REST (WebClient/RestClient) or gRPC for inter-service communication.
 */
public interface ShiftAssignmentClient {

    /**
     * Retrieve the shift assignment for a candidate within a specific exam and shift.
     *
     * @param candidateId the candidate's unique identifier
     * @param examId      the examination identifier
     * @param shiftId     the shift identifier
     * @param tenantId    the tenant (examination authority) identifier
     * @return the shift assignment details including paper reference and duration
     */
    ShiftAssignment getShiftAssignment(UUID candidateId, UUID examId, UUID shiftId, String tenantId);
}
