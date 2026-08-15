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

package com.examplatform.shared.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Fluent builder that wraps Spring's {@link ProblemDetail} to produce
 * RFC 7807-compliant error responses throughout the platform.
 *
 * <p>Usage example:
 * <pre>{@code
 * ProblemDetail problem = ExamPlatformProblemDetail
 *     .forStatus(HttpStatus.UNPROCESSABLE_ENTITY)
 *     .type(URI.create("https://errors.examplatform.gov.in/question/invalid-transition"))
 *     .title("Invalid State Transition")
 *     .detail("Cannot transition question from APPROVED to DRAFT")
 *     .instance(URI.create("/api/v1/questions/" + questionId + "/transition"))
 *     .property("currentState", "APPROVED")
 *     .property("targetState",  "DRAFT")
 *     .property("traceId",      traceId)
 *     .build();
 * }</pre>
 */
public final class ExamPlatformProblemDetail {

    // -----------------------------------------------------------------------
    // Internal builder state
    // -----------------------------------------------------------------------

    private final HttpStatus status;
    private URI               type;
    private String            title;
    private String            detail;
    private URI               instance;
    private final Map<String, Object> properties = new HashMap<>();

    private ExamPlatformProblemDetail(HttpStatus status) {
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
    public static ExamPlatformProblemDetail forStatus(HttpStatus status) {
        return new ExamPlatformProblemDetail(status);
    }

    /**
     * Convenience overload accepting a raw status code.
     *
     * @param statusCode HTTP status code (e.g. 422)
     * @return a new builder instance
     */
    public static ExamPlatformProblemDetail forStatus(int statusCode) {
        return forStatus(HttpStatus.valueOf(statusCode));
    }

    // -----------------------------------------------------------------------
    // Builder methods
    // -----------------------------------------------------------------------

    /**
     * Set the {@code type} URI identifying the problem type.
     * Should resolve to human-readable documentation.
     *
     * @param type absolute URI for the problem type
     * @return this builder
     */
    public ExamPlatformProblemDetail type(URI type) {
        this.type = type;
        return this;
    }

    /**
     * Set the short, human-readable summary of the problem.
     *
     * @param title problem title
     * @return this builder
     */
    public ExamPlatformProblemDetail title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Set a detailed, human-readable explanation specific to this occurrence.
     *
     * @param detail problem detail
     * @return this builder
     */
    public ExamPlatformProblemDetail detail(String detail) {
        this.detail = detail;
        return this;
    }

    /**
     * Set the specific URI reference identifying this problem occurrence.
     *
     * @param instance resource URI where the problem occurred
     * @return this builder
     */
    public ExamPlatformProblemDetail instance(URI instance) {
        this.instance = instance;
        return this;
    }

    /**
     * Add an extension property to the problem document.
     *
     * @param name  property name
     * @param value property value (must be JSON-serialisable)
     * @return this builder
     */
    public ExamPlatformProblemDetail property(String name, Object value) {
        this.properties.put(name, value);
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

        if (type != null) {
            problem.setType(type);
        }
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
