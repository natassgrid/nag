// src/utils/languagePreference.ts
// Persists candidate examination language preferences to localStorage and handles
// candidate profile preferred language resolution (English + 1 chosen Indian scheduled language).

const SESSION_KEY_PREFIX = 'nag_exam_lang_';
export const CANDIDATE_PREFERRED_LANG_KEY = 'nag_candidate_preferred_language';

/**
 * Retrieve the candidate's global preferred regional language (e.g. from candidate profile).
 * Defaults to 'hi' (Hindi) if not previously chosen.
 */
export function getCandidatePreferredRegionalLanguage(): string {
  try {
    const saved = localStorage.getItem(CANDIDATE_PREFERRED_LANG_KEY);
    return saved && saved !== 'en' ? saved : 'hi';
  } catch {
    return 'hi';
  }
}

/**
 * Persist the candidate's preferred regional language chosen in profile or exam setup.
 */
export function setCandidatePreferredRegionalLanguage(languageCode: string): void {
  try {
    if (languageCode) {
      localStorage.setItem(CANDIDATE_PREFERRED_LANG_KEY, languageCode);
    }
  } catch {
    // ignore
  }
}

/**
 * Persist the candidate's language choice for a specific exam session.
 * Keyed by sessionId so multiple sessions don't bleed into each other.
 */
export function saveLanguagePreference(sessionId: string, languageCode: string): void {
  try {
    localStorage.setItem(`${SESSION_KEY_PREFIX}${sessionId}`, languageCode);
  } catch {
    // localStorage may be unavailable (private browsing quota exceeded) — silently ignore
  }
}

/**
 * Retrieve the persisted language preference for a session.
 * Returns null if no preference has been stored.
 */
export function loadLanguagePreference(sessionId: string): string | null {
  try {
    return localStorage.getItem(`${SESSION_KEY_PREFIX}${sessionId}`);
  } catch {
    return null;
  }
}

/**
 * Remove the language preference for a session (called after submission).
 */
export function clearLanguagePreference(sessionId: string): void {
  try {
    localStorage.removeItem(`${SESSION_KEY_PREFIX}${sessionId}`);
  } catch {
    // ignore
  }
}
