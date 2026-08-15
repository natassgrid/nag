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

package com.examplatform.examination.exception;

import lombok.Getter;

/**
 * Thrown when a schedule state transition is invalid (wrong role, wrong current
 * state, or missing mandatory field such as changeReason on amendment).
 */
@Getter
public class ScheduleWorkflowException extends RuntimeException {

    private final String currentStatus;
    private final String targetStatus;

    public ScheduleWorkflowException(String currentStatus, String targetStatus) {
        super(String.format(
                "Invalid schedule transition: %s → %s is not permitted",
                currentStatus, targetStatus));
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public ScheduleWorkflowException(String message) {
        super(message);
        this.currentStatus = null;
        this.targetStatus = null;
    }
}
