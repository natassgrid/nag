// src/services/responseService.ts
// Wraps response-service REST calls for answer saving, session submission, and response history retrieval.

import { api, unwrap } from './api';
import { offlineQueue } from '../utils/offlineQueue';
import type {
  BulkSaveRequest,
  SaveResponseRequest,
  SaveResponseResponse,
} from '../types/api';

const BASE = '/api/v1/responses';

export interface PersistedResponse {
  id?: string;
  responseId?: string;
  sessionId?: string;
  questionId: string;
  selectedOptionIds?: string | number[] | string[];
  selectedOptionIndex?: number;
  enteredValue?: string | null;
  markedForReview?: boolean;
  revisionSequence?: number;
  timeTakenSeconds?: number;
  cumulativeTimeSpentMs?: number;
  saveSource?: string;
  savedAt?: string;
  timestamp?: string;
  isFinal?: boolean;
}

function formatBackendPayload(request: SaveResponseRequest) {
  const selectedOptionIds =
    request.selectedOptionIndex !== null && request.selectedOptionIndex !== undefined
      ? JSON.stringify([request.selectedOptionIndex])
      : null;

  return {
    questionId: request.questionId,
    selectedOptionIds,
    enteredValue: request.integerAnswer !== undefined && request.integerAnswer !== null ? String(request.integerAnswer) : null,
    timestamp: new Date().toISOString(),
    cumulativeTimeSpentMs: request.timeTakenSeconds ? request.timeTakenSeconds * 1000 : 5000,
    saveSource: 'MANUAL',
    revisionSequence: request.revisionSequence || 1,
    focusLossCount: 0,
  };
}

export const responseService = {
  /**
   * Save a single response for a question.
   * If the request fails due to network error, queues it offline for bulk-save.
   */
  async saveResponse(
    sessionId: string,
    request: SaveResponseRequest,
  ): Promise<SaveResponseResponse> {
    const payload = formatBackendPayload(request);
    try {
      return unwrap(await api.post(`${BASE}/${sessionId}/save`, payload));
    } catch (err: unknown) {
      const isNetworkError =
        !navigator.onLine ||
        (err instanceof Error &&
          (err.message.includes('Network Error') ||
            err.message.includes('timeout') ||
            err.message.includes('ECONNREFUSED')));

      if (isNetworkError) {
        // Queue for bulk save upon reconnect
        offlineQueue.enqueue(sessionId, request);
        return {
          responseId: `offline-${Date.now()}`,
          sessionId,
          questionId: request.questionId,
          revisionSequence: request.revisionSequence || 1,
          savedAt: new Date().toISOString(),
          status: 'QUEUED_OFFLINE',
        };
      }
      throw err;
    }
  },

  /**
   * Reconcile multiple responses queued offline.
   * Clears successfully processed items from the offline queue.
   */
  async reconcileOfflineQueue(sessionId: string): Promise<SaveResponseResponse[]> {
    const queued = offlineQueue.getQueue(sessionId);
    if (queued.length === 0) return [];

    const req: BulkSaveRequest = {
      responses: queued.map((item) => item.response),
    };

    const results = await this.bulkSave(sessionId, req);
    offlineQueue.clear(sessionId);
    return results;
  },

  /**
   * Bulk-save an array of responses (e.g., on reconnect after offline period).
   */
  async bulkSave(sessionId: string, request: BulkSaveRequest): Promise<SaveResponseResponse[]> {
    const formattedResponses = request.responses.map((r) => formatBackendPayload(r));
    return unwrap(
      await api.post(`${BASE}/${sessionId}/bulk-save`, { responses: formattedResponses }),
    );
  },

  /**
   * Submit an exam session for evaluation.
   * Triggers the EVALUATION_STARTED workflow.
   */
  async submitSession(sessionId: string): Promise<{ status: string; submittedAt: string }> {
    return unwrap(await api.post(`${BASE}/${sessionId}/submit`));
  },

  /**
   * Retrieve response history for a session (used to restore state on reconnect).
   */
  async getSessionResponses(sessionId: string): Promise<PersistedResponse[]> {
    return unwrap(await api.get(`${BASE}/${sessionId}`));
  },
};
