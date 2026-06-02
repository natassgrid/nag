package com.examplatform.shared.lifecycle;

/**
 * Lifecycle states for a {@code Paper} entity.
 *
 * <p>Valid transitions:
 * <pre>
 *   DRAFT     → APPROVED    (Exam Controller approves assembled paper)
 *   APPROVED  → ENCRYPTED   (HSM shift-key encryption applied; paper sealed for delivery)
 * </pre>
 *
 * <p>Once a paper reaches {@link #ENCRYPTED} it is immutable.
 * The encrypted package is stored in object storage; only a reference is held
 * by the Paper Generator service.
 */
public enum PaperState {

    /**
     * Paper has been assembled by the Paper Generator but has not yet
     * been reviewed or approved by an Exam Controller.
     */
    DRAFT,

    /**
     * Paper has been approved by an Exam Controller.
     * Statistical comparability checks (shift balance) must have passed.
     * Awaiting HSM encryption before delivery.
     */
    APPROVED,

    /**
     * Paper package has been encrypted with the shift-specific AES-256 key
     * managed by HashiCorp Vault / HSM.
     * The plaintext paper never exists outside memory from this point onward.
     */
    ENCRYPTED
}
