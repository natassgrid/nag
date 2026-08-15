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

package com.examplatform.shared.lifecycle;

/**
 * Lifecycle states for an {@code ExamSession}.
 *
 * <p>Valid transitions:
 * <pre>
 *   ACTIVE    → SUBMITTED  (candidate submits their responses)
 *   ACTIVE    → EXPIRED    (session timer elapses without submission)
 * </pre>
 *
 * <p>{@link #SUBMITTED} and {@link #EXPIRED} are terminal states; no further
 * transitions are permitted. Response sets for sessions in either state are
 * locked and immutable.
 */
public enum SessionState {

    /**
     * Session is currently in progress.
     * The candidate is answering questions; responses may still be saved.
     */
    ACTIVE,

    /**
     * Candidate explicitly submitted their response set before time expired.
     * All responses are finalised ({@code is_final = true}).
     */
    SUBMITTED,

    /**
     * Session timer elapsed before the candidate submitted.
     * The platform auto-finalises all saved responses.
     */
    EXPIRED
}
