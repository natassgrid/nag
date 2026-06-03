package com.examplatform.shared.lifecycle;

/**
 * Lifecycle states for an {@code ExamSession}.
 *
 * <p>Valid transitions:
 * <pre>
 *   ACTIVE    → SUBMITTED  (candidate submits their responses)
 *   ACTIVE    → EXPIRED    (session timer elapses without submission)
 * </pre>
 *
 * <p>{@link #SUBMITTED} and {@link #EXPIRED} are terminal states; no further
 * transitions are permitted. Response sets for sessions in either state are
 * locked and immutable.
 */
public enum SessionState {

    /**
     * Session is currently in progress.
     * The candidate is answering questions; responses may still be saved.
     */
    ACTIVE,

    /**
     * Candidate explicitly submitted their response set before time expired.
     * All responses are finalised ({@code is_final = true}).
     */
    SUBMITTED,

    /**
     * Session timer elapsed before the candidate submitted.
     * The platform auto-finalises all saved responses.
     */
    EXPIRED
}
