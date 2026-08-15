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

import java.time.LocalDate;

/**
 * Thrown when a proposed schedule date (exam or reserve) conflicts with
 * an existing schedule in the same tenant (Req 7b.10, 7b.11).
 */
public class ScheduleDateConflictException extends RuntimeException {

    public ScheduleDateConflictException(LocalDate date, String conflictType) {
        super(String.format(
                "Schedule date conflict: %s conflicts with an existing %s for this tenant",
                date, conflictType));
    }
}
