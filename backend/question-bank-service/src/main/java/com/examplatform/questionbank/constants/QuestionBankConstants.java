/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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

package com.examplatform.questionbank.constants;

/**
 * Constants used across question-bank-service module components.
 * Consolidates duplicate string literals within the module.
 */
public final class QuestionBankConstants {

    private QuestionBankConstants() {
        // Utility class, prevent instantiation
    }

    // --- Workflow & Lifecycle Topics ---
    public static final String TOPIC_QUESTION_LIFECYCLE = "exam.question.lifecycle";

    // --- Workflow Event Types ---
    public static final String EVENT_SUBMITTED_FOR_REVIEW = "SUBMITTED_FOR_REVIEW";
    public static final String EVENT_REVIEWER_APPROVED = "REVIEWER_APPROVED";
    public static final String EVENT_RETURNED_TO_DRAFT = "RETURNED_TO_DRAFT";
    public static final String EVENT_QUESTION_PUBLISHED = "QUESTION_PUBLISHED";

    // --- Notification Types ---
    public static final String NOTIF_QUESTION_ASSIGNED_FOR_REVIEW = "QUESTION_ASSIGNED_FOR_REVIEW";
    public static final String NOTIF_QUESTION_APPROVED_BY_REVIEWER = "QUESTION_APPROVED_BY_REVIEWER";
    public static final String NOTIF_QUESTION_RETURNED_FOR_REVISION = "QUESTION_RETURNED_FOR_REVISION";

    // --- Workflow Payload & Configuration Keys ---
    public static final String CONFIG_DUAL_REVIEW_REQUIRED = "question.dual.review.required";
    public static final String KEY_TYPE = "type";
    public static final String KEY_COMMENTS = "comments";
    public static final String KEY_ASSIGNED_REVIEWER = "assignedReviewer";
    public static final String KEY_ASSIGNED_REVIEWER_2 = "assignedReviewer2";
    public static final String KEY_ASSIGNED_REVIEWERS = "assignedReviewers";
    public static final String KEY_DUAL_REVIEW_REQUIRED = "dualReviewRequired";
    public static final String KEY_ESCALATED_TO_CONTROLLER = "escalatedToController";
    public static final String KEY_PUBLISHER_ID = "publisherId";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_TO_STATE = "toState";
    public static final String KEY_FROM_STATE = "fromState";

    // --- Default & Fallback Values ---
    public static final String VALUE_UNASSIGNED = "UNASSIGNED";
    public static final String DEFAULT_NO_COMMENTS = "No comments provided";

    // --- Entity & Service Identifiers ---
    public static final String QUESTION_SERVICE = "question_service";
    public static final String QUESTION_TYPE = "questionType";
    public static final String COGNITIVE_LEVEL = "cognitiveLevel";
    public static final String JSONB = "jsonb";
    public static final String CHAPTER = "chapter";
    public static final String QUESTION_NOT_FOUND = "Question not found: ";
    public static final String SUBJECT_ID = "subjectId";
    public static final String EXPLANATION = "explanation";
    public static final String ANSWER_KEY = "answerKey";
    public static final String TOPIC_ID = "topic_id";
    public static final String TOPIC_ID_CAMEL = "topicId";
    public static final String REVIEW_COMMENTS = "review_comments";
    public static final String UPDATED_AT = "updatedAt";
    public static final String TRANSLATION_ID = "translationId";
    public static final String BATCH_JOB_NOT_FOUND = "Batch job not found: ";
    public static final String TRANSLATION_NOT_FOUND = "Translation not found: ";
    public static final String SOURCE_QUESTION_NOT_FOUND = "Source question not found: ";
    public static final String PASSAGE_NOT_FOUND = "Passage not found: ";
}
