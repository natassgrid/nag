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
  responseId?: string;
  sessionId?: string;
  questionId: string;
  selectedOptionIndex?: number;
  markedForReview?: boolean;
  revisionSequence?: number;
  timeTakenSeconds?: number;
  savedAt?: string;
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
    try {
      return unwrap(await api.post(`${BASE}/${sessionId}/save`, request));
    } catch (error: unknown) {
      // Network offline — queue for later
      const axiosError = error as { code?: string };
      if (axiosError.code === 'ERR_NETWORK' || axiosError.code === 'ECONNABORTED') {
        offlineQueue.enqueue(sessionId, request);
        // Return a synthetic response so TakeExam can continue locally
        return {
          responseId: `offline-${Date.now()}`,
          questionId: request.questionId,
          savedAt: new Date().toISOString(),
          revisionSequence: request.revisionSequence,
        };
      }
      throw error;
    }
  },

  /**
   * Flush the offline queue for a session.
   * Called when connectivity is restored.
   */
  async flushOfflineQueue(sessionId: string): Promise<void> {
    const queued = offlineQueue.getForSession(sessionId);
    if (queued.length === 0) return;

    const bulkRequest: BulkSaveRequest = {
      responses: queued.map((q) => q.response),
    };

    await responseService.bulkSave(sessionId, bulkRequest);
    offlineQueue.clearSession(sessionId);
  },

  /** Bulk-save for offline-buffered responses with server-side deduplication. */
  async bulkSave(sessionId: string, request: BulkSaveRequest): Promise<SaveResponseResponse[]> {
    return unwrap(await api.post(`${BASE}/${sessionId}/bulk-save`, request));
  },

  /** Finalize and submit the exam session — locks all responses. */
  async submitSession(sessionId: string): Promise<void> {
    await api.post(`${BASE}/${sessionId}/submit`);
  },

  /** Retrieve previously saved responses for an exam session (for session resumption). */
  async getSessionResponses(sessionId: string): Promise<PersistedResponse[]> {
    try {
      return unwrap(await api.get(`${BASE}/${sessionId}/responses`));
    } catch {
      return [];
    }
  },
};
