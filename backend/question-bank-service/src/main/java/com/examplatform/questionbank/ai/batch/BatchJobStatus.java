/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.examplatform.questionbank.ai.batch;

/**
 * Status enum for batch question generation jobs.
 */
public enum BatchJobStatus {
    /** Job has been submitted and is queued for processing. */
    PENDING,
    /** Job is currently being processed (LLM calls in progress). */
    PROCESSING,
    /** Job completed successfully — all questions generated. */
    COMPLETED,
    /** Job completed with some failures — partial results available. */
    PARTIALLY_COMPLETED,
    /** Job failed entirely. */
    FAILED,
    /** Job was cancelled by the user. */
    CANCELLED
}
