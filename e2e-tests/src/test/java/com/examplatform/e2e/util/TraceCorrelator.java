/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) — Open Digital Public Infrastructure (DPI) Platform
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
 */
package com.examplatform.e2e.util;

import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extracts and logs distributed trace identifiers from REST Assured responses.
 *
 * <p>Checks for the {@code X-Trace-Id} header first (NAG internal convention),
 * then falls back to the W3C {@code traceparent} header. Returns the sentinel
 * value {@value #NO_TRACE} when neither header is present.
 */
public final class TraceCorrelator {

    private static final Logger LOG = LoggerFactory.getLogger(TraceCorrelator.class);

    /** Sentinel returned when no trace header is found in a response. */
    public static final String NO_TRACE = "no-trace";

    private static final String HEADER_X_TRACE_ID  = "X-Trace-Id";
    private static final String HEADER_TRACEPARENT  = "traceparent";

    private TraceCorrelator() {
        // utility class — no instances
    }

    /**
     * Extracts the trace identifier from a REST Assured response.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>{@code X-Trace-Id} — NAG gateway header</li>
     *   <li>{@code traceparent} — W3C Trace Context</li>
     *   <li>{@value #NO_TRACE} — if neither header is present</li>
     * </ol>
     *
     * @param response the REST Assured HTTP response
     * @return the trace ID string, or {@value #NO_TRACE}
     */
    public static String extractTraceId(Response response) {
        String traceId = response.getHeader(HEADER_X_TRACE_ID);
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        traceId = response.getHeader(HEADER_TRACEPARENT);
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        return NO_TRACE;
    }

    /**
     * Logs the trace identifier extracted from the given response.
     *
     * @param testName  display name of the current test method
     * @param response  the REST Assured HTTP response
     */
    public static void logTrace(String testName, Response response) {
        String traceId = extractTraceId(response);
        LOG.info("[TRACE] test={} traceId={}", testName, traceId);
    }
}
