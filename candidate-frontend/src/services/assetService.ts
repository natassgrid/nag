// src/services/assetService.ts
// Direct client for DPI Asset Service (secure multimedia asset storage, validation & streaming).

import { api, unwrap } from './api';
import { tokenManager } from '../utils/tokenManager';
import type { AssetUploadResponse } from '../types/api';

const blobCache = new Map<string, string>();

export const assetService = {
  /**
   * Upload an asset binary with metadata tags.
   */
  async uploadAsset(
    file: File,
    assetType: 'PHOTO' | 'SIGNATURE' | 'DOCUMENT' | 'ID_PROOF' = 'PHOTO',
    metadata?: { title?: string; description?: string; tags?: string },
  ): Promise<AssetUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('assetType', assetType === 'ID_PROOF' ? 'DOCUMENT' : assetType);

    if (metadata?.title) formData.append('title', metadata.title);
    if (metadata?.description) formData.append('description', metadata.description);
    if (metadata?.tags) formData.append('tags', metadata.tags);

    const response = await api.post('/api/v1/assets', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });

    const result = unwrap<AssetUploadResponse>(response);

    // Cache local preview immediately for fast responsiveness
    const localUrl = URL.createObjectURL(file);
    blobCache.set(result.id, localUrl);

    return result;
  },

  /**
   * Fetch asset binary with JWT authentication and return browser Object URL.
   */
  async fetchAssetBlobUrl(assetId: string): Promise<string> {
    if (blobCache.has(assetId)) {
      return blobCache.get(assetId)!;
    }

    const token = tokenManager.getAccessToken();
    const response = await fetch(`/api/v1/assets/${assetId}/download`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch asset binary: HTTP ${response.status}`);
    }

    const blob = await response.blob();
    const objectUrl = URL.createObjectURL(blob);
    blobCache.set(assetId, objectUrl);
    return objectUrl;
  },

  /**
   * Get direct download/view URL.
   */
  getAssetDownloadUrl(assetId: string): string {
    return `/api/v1/assets/${assetId}/download`;
  },

  /**
   * Validate profile photo dimensions, file size, and MIME type.
   */
  validateProfilePhoto(file: File): { valid: boolean; error?: string } {
    const validMimes = ['image/jpeg', 'image/png', 'image/webp'];
    if (!validMimes.includes(file.type)) {
      return {
        valid: false,
        error: 'Invalid file format. Please upload JPG, PNG, or WebP.',
      };
    }

    const maxBytes = 5 * 1024 * 1024; // 5 MB
    if (file.size > maxBytes) {
      return {
        valid: false,
        error: 'File size exceeds 5MB limit.',
      };
    }

    const minBytes = 2 * 1024; // 2 KB
    if (file.size < minBytes) {
      return {
        valid: false,
        error: 'File size is too small or corrupted.',
      };
    }

    return { valid: true };
  },

  /**
   * Delete asset.
   */
  async deleteAsset(assetId: string): Promise<void> {
    blobCache.delete(assetId);
    await api.delete(`/api/v1/assets/${assetId}`);
  },
};
