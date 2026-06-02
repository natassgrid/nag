package com.examplatform.shared.audit;

/**
 * Enumeration of every audit event type produced across all services in the
 * Open Source Government Examination Platform.
 *
 * <p>These values are persisted in the {@code audit_event.event_type} column and
 * must remain stable across releases. Rename only via a migration that updates
 * existing rows.
 *
 * <p>Grouped by domain for readability:
 * <ul>
 *   <li><strong>Identity / Access</strong>: LOGIN, LOGOUT, ROLE_CHANGE, DENIED_ACCESS,
 *       ACCOUNT_LOCK, KEY_REVOCATION
 *   <li><strong>Candidate</strong>: CANDIDATE_PROFILE_CREATED
 *   <li><strong>Question Bank</strong>: QUESTION_CREATED, QUESTION_STATE_TRANSITION
 *   <li><strong>Paper</strong>: PAPER_GENERATED, PAPER_APPROVED
 *   <li><strong>Delivery / Session</strong>: SESSION_STARTED, SESSION_SUBMITTED
 *   <li><strong>Evaluation / Result</strong>: EVALUATION_CREATED, RESULT_PUBLISHED
 *   <li><strong>Admin</strong>: CONFIG_CHANGED
 *   <li><strong>Security</strong>: TAMPER_ATTEMPT
 * </ul>
 */
public enum AuditEventType {

    // -----------------------------------------------------------------------
    // Identity / Access domain
    // -----------------------------------------------------------------------

    /** A user (candidate or staff) successfully authenticated. */
    LOGIN,

    /** A user explicitly ended their session. */
    LOGOUT,

    /** A role was assigned to or revoked from a user account. */
    ROLE_CHANGE,

    /** An authorisation check failed (HTTP 403). */
    DENIED_ACCESS,

    /** An account was locked after repeated authentication failures. */
    ACCOUNT_LOCK,

    /** An HSM/Vault encryption key was revoked by a Security Admin. */
    KEY_REVOCATION,

    // -----------------------------------------------------------------------
    // Candidate domain
    // -----------------------------------------------------------------------

    /** A new candidate profile was created (registration completed). */
    CANDIDATE_PROFILE_CREATED,

    // -----------------------------------------------------------------------
    // Question Bank domain
    // -----------------------------------------------------------------------

    /** A new question was created and persisted in DRAFT state. */
    QUESTION_CREATED,

    /** A question transitioned between lifecycle states (e.g. DRAFT → REVIEW). */
    QUESTION_STATE_TRANSITION,

    // -----------------------------------------------------------------------
    // Paper domain
    // -----------------------------------------------------------------------

    /** A question paper was successfully assembled by the Paper Generator. */
    PAPER_GENERATED,

    /** A question paper was approved by an Exam Controller. */
    PAPER_APPROVED,

    // -----------------------------------------------------------------------
    // Delivery / Session domain
    // -----------------------------------------------------------------------

    /** A candidate started an examination session. */
    SESSION_STARTED,

    /** A candidate submitted (finalised) their examination session. */
    SESSION_SUBMITTED,

    // -----------------------------------------------------------------------
    // Evaluation / Result domain
    // -----------------------------------------------------------------------

    /** An evaluation record was created (auto or manual). */
    EVALUATION_CREATED,

    /** Examination results were published and made available to candidates. */
    RESULT_PUBLISHED,

    // -----------------------------------------------------------------------
    // Admin domain
    // -----------------------------------------------------------------------

    /** A platform configuration parameter was changed via the Admin API. */
    CONFIG_CHANGED,

    // -----------------------------------------------------------------------
    // Security domain
    // -----------------------------------------------------------------------

    /**
     * A tamper attempt was detected — e.g. an attempt to UPDATE or DELETE an
     * immutable audit record, or a signature verification failure.
     */
    TAMPER_ATTEMPT
}
