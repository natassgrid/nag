// src/services/sessionService.ts
// Wraps delivery-service REST calls for exam session lifecycle.

import { api } from './api';
import type {
  NavigationRequest,
  NavigationResponse,
  SessionStartRequest,
  SessionStartResponse,
} from '../types/api';

const BASE = '/api/v1/sessions';

export const sessionService = {
  /**
   * Start an exam session for the authenticated candidate.
   * Returns sessionId, all questions (without correct answers), and timing info.
   */
  async startSession(request: SessionStartRequest): Promise<SessionStartResponse> {
    return (await api.post<SessionStartResponse>(`${BASE}/start`, request)).data;
  },

  /**
   * Navigate within an active session.
   * The backend enforces navigation policy (sequential / flexible / restricted).
   */
  async navigate(
    sessionId: string,
    request: Omit<NavigationRequest, 'sessionId'>,
  ): Promise<NavigationResponse> {
    return (
      await api.post<NavigationResponse>(`${BASE}/${sessionId}/navigate`, {
        ...request,
        sessionId,
      })
    ).data;
  },

  /** Send a webcam snapshot to the proctoring service. */
  async captureSnapshot(sessionId: string, imageBlob: Blob): Promise<void> {
    const arrayBuffer = await imageBlob.arrayBuffer();
    await api.post(`${BASE}/${sessionId}/proctoring/snapshot`, arrayBuffer, {
      headers: { 'Content-Type': 'application/octet-stream' },
    });
  },

  /** Record a fullscreen-exit event (backend flags session after 3 exits). */
  async recordFullScreenExit(sessionId: string): Promise<void> {
    await api.post(`${BASE}/${sessionId}/proctoring/fullscreen-exit`);
  },
};
