/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 Open Digital Public Infrastructure (DPI) Platform Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 */
package com.examplatform.questionbank.ai.generation;

/**
 * Execution mode for AI question generation pipeline.
 *
 * <ul>
 *   <li>{@link #AUTO} - Conditionally triage between Single-Model Fast Path and Multi-Agent Path.</li>
 *   <li>{@link #FAST} - Single-Model Fast Path (economical, minimal latency).</li>
 *   <li>{@link #MULTI_AGENT} - Deep Review Multi-Agent collaborative pipeline with psychometric critic.</li>
 * </ul>
 */
public enum ExecutionMode {
    AUTO,
    FAST,
    MULTI_AGENT
}
