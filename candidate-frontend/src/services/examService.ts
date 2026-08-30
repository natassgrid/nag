// src/services/examService.ts
// Wraps examination-service REST calls for candidate-facing exam operations.

import { api, unwrap } from './api';
import type {
  ExamApplicationRequest,
  ExamApplicationResponse,
  ExaminationResponse,
  Page,
} from '../types/api';

const BASE = '/api/v1/examinations';

export const examService = {
  /**
   * List all PUBLISHED exams — public endpoint, no auth required.
   * Supports search and pagination.
   */
  async listPublishedExams(params?: {
    search?: string;
    page?: number;
    size?: number;
  }): Promise<Page<ExaminationResponse>> {
    return unwrap(
      await api.get(`${BASE}/public`, {
        params: { page: 0, size: 20, ...params },
      }),
    );
  },

  /** Get a single published exam's details. */
  async getExam(examId: string): Promise<ExaminationResponse> {
    return unwrap(await api.get(`${BASE}/public/${examId}`));
  },

  /** Apply for an exam — requires CANDIDATE role. */
  async applyForExam(request: ExamApplicationRequest): Promise<ExamApplicationResponse> {
    return unwrap(await api.post(`${BASE}/${request.examId}/apply`, {}));
  },

  /** Get exams the authenticated candidate has applied for. */
  async getMyExams(): Promise<ExamApplicationResponse[]> {
    return unwrap(await api.get(`${BASE}/my-exams`));
  },

  /** Get the application status for a specific exam. */
  async getApplicationStatus(examId: string): Promise<ExamApplicationResponse> {
    return unwrap(await api.get(`${BASE}/${examId}/my-application`));
  },
};
