// src/services/candidateService.ts
// Wraps all candidate-service REST calls for profile management.

import { api, unwrap } from './api';
import { tokenManager } from '../utils/tokenManager';
import type {
  AssetUploadResponse,
  CandidateEducation,
  CandidateEducationRequest,
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
  async getProfile(userId?: string | null): Promise<CandidateProfileResponse> {
    const id = userId || tokenManager.getUserId();
    if (!id || id === 'undefined' || id === 'null') {
      throw new Error('Valid userId is required to fetch candidate profile');
    }
    return (await api.get<CandidateProfileResponse>(`${BASE}/${id}`)).data;
  },

  /** Update candidate profile fields. */
  async updateProfile(
    userId?: string | null,
    request?: UpdateCandidateProfileRequest,
  ): Promise<CandidateProfileResponse> {
    const id = userId || tokenManager.getUserId();
    if (!id || id === 'undefined' || id === 'null') {
      throw new Error('Valid userId is required to update candidate profile');
    }
    return (await api.put<CandidateProfileResponse>(`${BASE}/${id}`, request || {})).data;
  },

  /** Update candidate profile photo asset ID. */
  async updateProfilePhoto(userId: string, photoAssetId: string): Promise<CandidateProfileResponse> {
    return (await api.put<CandidateProfileResponse>(`${BASE}/${userId}`, { photoAssetId })).data;
  },

  /** Fetch all educational records for candidate. */
  async getEducation(userId?: string | null): Promise<CandidateEducation[]> {
    const id = userId || tokenManager.getUserId();
    if (!id || id === 'undefined' || id === 'null') {
      throw new Error('Valid userId is required to fetch educational details');
    }
    return (await api.get<CandidateEducation[]>(`${BASE}/${id}/education`)).data;
  },

  /** Add an educational detail record. */
  async addEducation(
    userId: string | null | undefined,
    request: CandidateEducationRequest,
  ): Promise<CandidateEducation> {
    const id = userId || tokenManager.getUserId();
    if (!id || id === 'undefined' || id === 'null') {
      throw new Error('Valid userId is required to add educational detail');
    }
    return (await api.post<CandidateEducation>(`${BASE}/${id}/education`, request)).data;
  },

  /** Update an educational detail record. */
  async updateEducation(
    userId: string | null | undefined,
    educationId: string,
    request: CandidateEducationRequest,
  ): Promise<CandidateEducation> {
    const id = userId || tokenManager.getUserId();
    if (!id || id === 'undefined' || id === 'null') {
      throw new Error('Valid userId is required to update educational detail');
    }
    return (await api.put<CandidateEducation>(`${BASE}/${id}/education/${educationId}`, request)).data;
  },

  /** Delete an educational detail record. */
  async deleteEducation(userId: string | null | undefined, educationId: string): Promise<void> {
    const id = userId || tokenManager.getUserId();
    if (!id || id === 'undefined' || id === 'null') {
      throw new Error('Valid userId is required to delete educational detail');
    }
    await api.delete(`${BASE}/${id}/education/${educationId}`);
  },

  /** Record explicit biometric consent before face/document collection. */
  async recordConsent(userId?: string | null, request?: ConsentRequest): Promise<void> {
    const id = userId || tokenManager.getUserId();
    if (!id || id === 'undefined' || id === 'null') {
      throw new Error('Valid userId is required to record consent');
    }
    await api.post(`${BASE}/${id}/consent`, request || { consentGiven: true });
  },

  /** Trigger DigiLocker document verification. */
  async verifyDigiLocker(userId?: string | null): Promise<{ status: string }> {
    const id = userId || tokenManager.getUserId();
    if (!id || id === 'undefined' || id === 'null') {
      throw new Error('Valid userId is required for DigiLocker verification');
    }
    return (await api.post<{ status: string }>(`${BASE}/${id}/digilocker/verify`)).data;
  },

  /**
   * Upload a candidate document (photo, signature, id-proof, certificate) to asset-service.
   */
  async uploadDocument(file: File): Promise<AssetUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return unwrap(
      await api.post('/api/v1/assets', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      }),
    );
  },

  /** Get binary download / view URL for an uploaded asset. */
  getAssetDownloadUrl(assetId: string): string {
    return `/api/v1/assets/${assetId}/download`;
  },

  /** Request erasure of all PII (DPDP right to be forgotten). */
  async erasePii(userId?: string | null): Promise<void> {
    const id = userId || tokenManager.getUserId();
    if (!id || id === 'undefined' || id === 'null') {
      throw new Error('Valid userId is required for PII erasure');
    }
    await api.delete(`${BASE}/${id}/pii`);
  },
};
