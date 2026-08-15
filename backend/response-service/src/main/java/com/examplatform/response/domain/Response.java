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

package com.examplatform.response.domain;

import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a single candidate response (or revision) to a question within an exam session.
 *
 * <p>The response table is Range-partitioned by {@code created_at} (monthly).
 * A composite index on (session_id, question_id, revision_sequence DESC) allows
 * efficient "latest answer" lookups. A partial index on candidate_id WHERE is_final=TRUE
 * supports final-response aggregation queries.
 */
@Entity
@Table(name = "response")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Response extends BaseEntity {

    /**
     * References the live exam session (from delivery-service).
     */
    @Column(name = "session_id", nullable = false, columnDefinition = "uuid")
    private UUID sessionId;

    /**
     * References the question being answered.
     */
    @Column(name = "question_id", nullable = false, columnDefinition = "uuid")
    private UUID questionId;

    /**
     * References the candidate who submitted the response.
     */
    @Column(name = "candidate_id", nullable = false, columnDefinition = "uuid")
    private UUID candidateId;

    /**
     * JSON array of selected option IDs (for MCQ/MRQ questions).
     * Stored as JSONB in PostgreSQL.
     */
    @Column(name = "selected_option_ids", columnDefinition = "jsonb")
    private String selectedOptionIds;

    /**
     * Free-text or computed value entered by the candidate (for subjective/numerical questions).
     */
    @Column(name = "entered_value", columnDefinition = "text")
    private String enteredValue;

    /**
     * Timestamp when this response revision was captured on the client.
     */
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    /**
     * Total cumulative time the candidate has spent on this question (milliseconds).
     */
    @Column(name = "cumulative_time_spent_ms", nullable = false)
    private long cumulativeTimeSpentMs;

    /**
     * Monotonically increasing revision number for this (session, question) pair.
     * Revision 1 = first answer, each subsequent save increments by 1.
     */
    @Column(name = "revision_sequence", nullable = false)
    private int revisionSequence;

    /**
     * Source of the save action: AUTO, MANUAL, NAVIGATION, OFFLINE.
     */
    @Column(name = "save_source", nullable = false, length = 20)
    private String saveSource;

    /**
     * Whether this response represents the candidate's final submitted answer.
     * Set to TRUE only on exam submission or explicit "mark as final" action.
     */
    @Column(name = "is_final", nullable = false)
    private boolean isFinal;
}
