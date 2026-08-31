// src/services/resultService.ts
// Wraps result-service REST calls for result retrieval and scorecard download.

import { api } from './api';
import type { ResultDto } from '../types/api';

const BASE = '/api/v1/results';

export const resultService = {
  /**
   * Fetch the result for a candidate in a specific exam.
   * Both the candidate themselves and SUPER_ADMIN can access this.
   */
  async getResult(candidateId: string, examId: string): Promise<ResultDto> {
    return (await api.get<ResultDto>(`${BASE}/${candidateId}`, { params: { examId } })).data;
  },

  /**
   * Download the scorecard PDF for a candidate.
   * Returns a Blob that can be used to create an object URL for download.
   */
  async downloadScorecard(candidateId: string): Promise<Blob> {
    const response = await api.get(`${BASE}/${candidateId}/scorecard`, {
      responseType: 'blob',
    });
    return response.data as Blob;
  },

  /** Trigger browser download of the scorecard PDF. */
  downloadScorecardFile(blob: Blob, candidateId: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `scorecard-${candidateId}.pdf`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  },
};
