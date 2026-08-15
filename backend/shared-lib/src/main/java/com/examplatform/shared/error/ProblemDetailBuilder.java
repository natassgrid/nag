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

package com.examplatform.shared.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Fluent RFC 7807 builder that wraps Spring's {@link ProblemDetail}.
 *
 * <p>Usage example:
 * <pre>{@code
 * ProblemDetail problem = ProblemDetailBuilder
 *     .forStatus(HttpStatus.UNPROCESSABLE_ENTITY)
 *     .withTitle("Invalid State Transition")
 *     .withDetail("Cannot transition question from APPROVED to DRAFT")
 *     .withInstance(URI.create("/api/v1/questions/" + questionId + "/transition"))
 *     .withProperty("currentState", "APPROVED")
 *     .withProperty("traceId", traceId)
 *     .build();
 * }</pre>
 */
public final class ProblemDetailBuilder {

    // -----------------------------------------------------------------------
    // Internal builder state
    // -----------------------------------------------------------------------

    private final HttpStatus             status;
    private String                       title;
    private String                       detail;
    private URI                          instance;
    private final Map<String, Object>    properties = new HashMap<>();

    private ProblemDetailBuilder(HttpStatus status) {
        this.status = status;
    }

    // -----------------------------------------------------------------------
    // Entry point
    // -----------------------------------------------------------------------

    /**
     * Start building a {@link ProblemDetail} for the given HTTP status.
     *
     * @param status HTTP status for this problem
     * @return a new builder instance
     */
    public static ProblemDetailBuilder forStatus(HttpStatus status) {
        return new ProblemDetailBuilder(status);
    }

    // -----------------------------------------------------------------------
    // Builder methods
    // -----------------------------------------------------------------------

    /**
     * Set the short, human-readable summary of the problem.
     *
     * @param title problem title
     * @return this builder
     */
    public ProblemDetailBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Set a detailed, human-readable explanation specific to this occurrence.
     *
     * @param detail problem detail
     * @return this builder
     */
    public ProblemDetailBuilder withDetail(String detail) {
        this.detail = detail;
        return this;
    }

    /**
     * Set the specific URI reference identifying this problem occurrence.
     *
     * @param instance resource URI where the problem occurred
     * @return this builder
     */
    public ProblemDetailBuilder withInstance(URI instance) {
        this.instance = instance;
        return this;
    }

    /**
     * Add an extension property to the problem document.
     *
     * @param key   property name
     * @param value property value (must be JSON-serialisable)
     * @return this builder
     */
    public ProblemDetailBuilder withProperty(String key, Object value) {
        this.properties.put(key, value);
        return this;
    }

    // -----------------------------------------------------------------------
    // Terminal operation
    // -----------------------------------------------------------------------

    /**
     * Build and return the {@link ProblemDetail} instance.
     *
     * @return configured {@link ProblemDetail}
     */
    public ProblemDetail build() {
        ProblemDetail problem = ProblemDetail.forStatus(status);

        if (title != null) {
            problem.setTitle(title);
        }
        if (detail != null) {
            problem.setDetail(detail);
        }
        if (instance != null) {
            problem.setInstance(instance);
        }

        // Merge extension properties
        properties.forEach(problem::setProperty);

        return problem;
    }
}
