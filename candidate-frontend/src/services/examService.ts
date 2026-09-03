// src/services/examService.ts
// Wraps examination-service REST calls for candidate-facing exam operations.

import { api, unwrap } from './api';
import type {
  AdmitCardResponse,
  ExamApplicationRequest,
  ExamApplicationResponse,
  ExaminationResponse,
  Page,
  PublicCentreResponse,
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

  /** List active examination centres across India for preference selection. */
  async listPublicCentres(params?: {
    state?: string;
    city?: string;
  }): Promise<PublicCentreResponse[]> {
    return unwrap(
      await api.get(`${BASE}/centres/public`, {
        params,
      }),
    );
  },

  /** Apply for an exam — requires CANDIDATE role with centre and shift preferences. */
  async applyForExam(request: ExamApplicationRequest): Promise<ExamApplicationResponse> {
    return unwrap(
      await api.post(`${BASE}/${request.examId}/apply`, {
        firstChoiceCentreId: request.firstChoiceCentreId,
        secondChoiceCentreId: request.secondChoiceCentreId,
        thirdChoiceCentreId: request.thirdChoiceCentreId,
        preferredShiftId: request.preferredShiftId,
        pwdRequired: request.pwdRequired,
        scribeRequired: request.scribeRequired,
      }),
    );
  },

  /** Get exams the authenticated candidate has applied for. */
  async getMyExams(): Promise<ExamApplicationResponse[]> {
    return unwrap(await api.get(`${BASE}/my-exams`));
  },

  /** Get the application status for a specific exam. */
  async getApplicationStatus(examId: string): Promise<ExamApplicationResponse> {
    return unwrap(await api.get(`${BASE}/${examId}/my-application`));
  },

  /** Get Admit Card / Hall Ticket by Exam ID. */
  async getAdmitCard(examId: string): Promise<AdmitCardResponse> {
    return unwrap(await api.get(`${BASE}/${examId}/admit-card`));
  },

  /** Get Admit Card / Hall Ticket by Application ID. */
  async getAdmitCardByApplicationId(applicationId: string): Promise<AdmitCardResponse> {
    return unwrap(await api.get(`${BASE}/applications/${applicationId}/admit-card`));
  },
};
