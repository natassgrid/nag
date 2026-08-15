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
 * Lifecycle states for a {@code Translation} entity.
 *
 * <p>Valid transitions:
 * <pre>
 *   DRAFT    → APPROVED   (reviewer approves)
 *   APPROVED → STALE      (source question modified after approval)
 *   STALE    → DRAFT      (translator picks up re-translation work)
 * </pre>
 */
public enum TranslationState {

    /**
     * Translation is in progress by a Translator.
     * Not yet reviewed or approved.
     */
    DRAFT,

    /**
     * Translation has been reviewed and approved.
     * Available for use in paper generation for the target language.
     */
    APPROVED,

    /**
     * The source question was modified after this translation was approved.
     * The translation is no longer guaranteed to be accurate and must be
     * re-reviewed before it can be used in new papers.
     */
    STALE
}
