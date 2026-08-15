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

package com.examplatform.shared.util;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;

import java.util.UUID;

/**
 * Platform-wide UUID v7 generator.
 *
 * <h3>Why UUID v7?</h3>
 * <ul>
 *   <li><strong>Time-ordered</strong> — the 48-bit Unix timestamp ms prefix makes
 *       UUIDs monotonically increasing within a millisecond window, giving
 *       near-sequential B-tree inserts and eliminating index page-splits that
 *       random v4 UUIDs cause at scale.</li>
 *   <li><strong>K-sortable</strong> — natural sort order matches insertion order,
 *       which benefits range scans and cursor-based pagination.</li>
 *   <li><strong>Globally unique</strong> — 74 bits of randomness after the
 *       timestamp; collision probability is negligible even across 500k
 *       concurrent writers.</li>
 *   <li><strong>Drop-in for v4</strong> — same {@link UUID} type; no schema
 *       changes required beyond removing the {@code DEFAULT gen_random_uuid()}
 *       clause (IDs are now always assigned by the application before insert).</li>
 * </ul>
 *
 * <h3>Thread safety</h3>
 * <p>{@link TimeBasedEpochGenerator} from {@code java-uuid-generator} is
 * thread-safe. The single shared instance is safe to use from virtual threads
 * (Project Loom) without external synchronisation.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * UUID id = UuidV7Generator.generate();
 * }</pre>
 */
public final class UuidV7Generator {

    /** Shared, thread-safe UUID v7 generator (time-based epoch variant). */
    private static final TimeBasedEpochGenerator GENERATOR =
            Generators.timeBasedEpochGenerator();

    private UuidV7Generator() {
        // utility class — no instances
    }

    /**
     * Generates a new UUID v7.
     *
     * @return a time-ordered, globally unique {@link UUID}
     */
    public static UUID generate() {
        return GENERATOR.generate();
    }
}
