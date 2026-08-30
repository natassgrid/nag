// src/services/candidateService.ts
// Wraps all candidate-service REST calls for profile management.

import { api, unwrap } from './api';
import type {
  AssetUploadResponse,
  CandidateProfileResponse,
  ConsentRequest,
  CreateCandidateProfileRequest,
  UpdateCandidateProfileRequest,
} from '../types/api';

const BASE = '/api/v1/candidates';

export const candidateService = {
  /** Create a new candidate profile after OTP verification. */
  async createProfile(request: CreateCandidateProfileRequest): Promise<CandidateProfileResponse> {
    return (await api.post<CandidateProfileResponse>(BASE, request)).data;
  },

  /** Fetch candidate profile by userId (UUID). */
  async getProfile(userId: string): Promise<CandidateProfileResponse> {
    return (await api.get<CandidateProfileResponse>(`${BASE}/${userId}`)).data;
  },

  /** Update candidate profile fields. */
  async updateProfile(
    userId: string,
    request: UpdateCandidateProfileRequest,
  ): Promise<CandidateProfileResponse> {
    return (await api.put<CandidateProfileResponse>(`${BASE}/${userId}`, request)).data;
  },

  /** Record explicit biometric consent before face/document collection. */
  async recordConsent(userId: string, request: ConsentRequest): Promise<void> {
    await api.post(`${BASE}/${userId}/consent`, request);
  },

  /** Trigger DigiLocker document verification. */
  async verifyDigiLocker(userId: string): Promise<{ status: string }> {
    return (await api.post<{ status: string }>(`${BASE}/${userId}/digilocker/verify`)).data;
  },

  /**
   * Upload a candidate document (photo, signature, id-proof) to asset-service.
   * The `referenceType` param tells asset-service how to tag the file.
   */
  async uploadDocument(
    file: File,
    referenceType: 'PHOTO' | 'SIGNATURE' | 'ID_PROOF',
  ): Promise<AssetUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('referenceType', referenceType);
    return unwrap(
      await api.post('/api/v1/assets', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      }),
    );
  },

  /** Request erasure of all PII (DPDP right to be forgotten). */
  async erasePii(userId: string): Promise<void> {
    await api.delete(`${BASE}/${userId}/pii`);
  },
};
