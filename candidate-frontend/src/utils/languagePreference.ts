// src/utils/languagePreference.ts
// Persists the candidate's chosen examination medium to localStorage so that
// the preference survives page refreshes during an exam session (e.g. after
// a network interruption that triggers a browser reload).

const KEY_PREFIX = 'nag_exam_lang_';

/**
 * Persist the candidate's language choice for a specific exam session.
 * Keyed by sessionId so multiple sessions don't bleed into each other.
 */
export function saveLanguagePreference(sessionId: string, languageCode: string): void {
  try {
    localStorage.setItem(`${KEY_PREFIX}${sessionId}`, languageCode);
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
    return localStorage.getItem(`${KEY_PREFIX}${sessionId}`);
  } catch {
    return null;
  }
}

/**
 * Remove the language preference for a session (called after submission).
 */
export function clearLanguagePreference(sessionId: string): void {
  try {
    localStorage.removeItem(`${KEY_PREFIX}${sessionId}`);
  } catch {
    // ignore
  }
}
