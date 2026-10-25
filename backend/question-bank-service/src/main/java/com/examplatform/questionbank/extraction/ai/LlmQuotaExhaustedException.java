/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 Open Digital Public Infrastructure (DPI) Platform Contributors
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
package com.examplatform.questionbank.extraction.ai;

/**
 * Thrown by the page extraction client when the upstream LLM gateway keeps returning
 * HTTP 429 / quota-exceeded responses after all backoff retries are exhausted.
 * The extraction worker treats this as a resumable checkpoint rather than a page failure.
 */
public class LlmQuotaExhaustedException extends RuntimeException {

    private final int pageNumber;

    public LlmQuotaExhaustedException(int pageNumber, String message, Throwable cause) {
        super(message, cause);
        this.pageNumber = pageNumber;
    }

    public int getPageNumber() {
        return pageNumber;
    }
}
