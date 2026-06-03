package com.examplatform.shared.lifecycle;

/**
 * Lifecycle states for a {@code Translation} entity.
 *
 * <p>Valid transitions:
 * <pre>
 *   DRAFT    → APPROVED   (reviewer approves)
 *   APPROVED → STALE      (source question modified after approval)
 *   STALE    → DRAFT      (translator picks up re-translation work)
 * </pre>
 */
public enum TranslationState {

    /**
     * Translation is in progress by a Translator.
     * Not yet reviewed or approved.
     */
    DRAFT,

    /**
     * Translation has been reviewed and approved.
     * Available for use in paper generation for the target language.
     */
    APPROVED,

    /**
     * The source question was modified after this translation was approved.
     * The translation is no longer guaranteed to be accurate and must be
     * re-reviewed before it can be used in new papers.
     */
    STALE
}
