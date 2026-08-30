// src/utils/offlineQueue.ts
// Buffers exam responses in localStorage when the network is unavailable.
// On reconnect, TakeExam calls flushQueue() which triggers bulk-save.

import type { SaveResponseRequest } from '../types/api';

const QUEUE_KEY = 'nag_offline_response_queue';

export interface QueuedResponse {
  sessionId: string;
  response: SaveResponseRequest;
  queuedAt: number; // epoch ms
}

export const offlineQueue = {
  enqueue(sessionId: string, response: SaveResponseRequest): void {
    const queue = offlineQueue.getAll();
    queue.push({ sessionId, response, queuedAt: Date.now() });
    localStorage.setItem(QUEUE_KEY, JSON.stringify(queue));
  },

  getAll(): QueuedResponse[] {
    try {
      const raw = localStorage.getItem(QUEUE_KEY);
      return raw ? JSON.parse(raw) : [];
    } catch {
      return [];
    }
  },

  getForSession(sessionId: string): QueuedResponse[] {
    return offlineQueue.getAll().filter((q) => q.sessionId === sessionId);
  },

  clearSession(sessionId: string): void {
    const remaining = offlineQueue.getAll().filter((q) => q.sessionId !== sessionId);
    localStorage.setItem(QUEUE_KEY, JSON.stringify(remaining));
  },

  clear(): void {
    localStorage.removeItem(QUEUE_KEY);
  },

  hasItems(sessionId: string): boolean {
    return offlineQueue.getForSession(sessionId).length > 0;
  },
};
