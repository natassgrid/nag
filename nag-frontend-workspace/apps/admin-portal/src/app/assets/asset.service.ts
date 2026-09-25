/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpEvent, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  AssetResponse,
  AssetMetadataUpdate,
  AssetSearchParams,
  ApiResponse,
  PaginatedAssetResponse,
} from './asset.model';

@Injectable({ providedIn: 'root' })
export class AssetService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/assets';

  /**
   * Upload an asset file with progress tracking events.
   */
  uploadWithProgress(file: File): Observable<HttpEvent<ApiResponse<AssetUploadPayload>>> {
    const formData = new FormData();
    formData.append('file', file);

    const req = new HttpRequest('POST', this.baseUrl, formData, {
      reportProgress: true,
    });
    return this.http.request<ApiResponse<AssetUploadPayload>>(req);
  }

  /**
   * Direct asset upload without progress events.
   */
  upload(file: File): Observable<AssetResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http
      .post<ApiResponse<AssetResponse>>(this.baseUrl, formData)
      .pipe(map((res) => res.data));
  }

  /**
   * Get single asset metadata by ID.
   */
  getAsset(id: string): Observable<AssetResponse> {
    return this.http
      .get<ApiResponse<AssetResponse>>(`${this.baseUrl}/${id}`)
      .pipe(map((res) => res.data));
  }

  /**
   * List assets with pagination.
   */
  listAssets(page = 0, size = 20): Observable<PaginatedAssetResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http
      .get<ApiResponse<PaginatedAssetResponse>>(this.baseUrl, { params })
      .pipe(map((res) => res.data));
  }

  /**
   * Search and filter assets with multi-criteria query parameters.
   */
  searchAssets(filter: AssetSearchParams): Observable<PaginatedAssetResponse> {
    let params = new HttpParams()
      .set('page', (filter.page ?? 0).toString())
      .set('size', (filter.size ?? 20).toString());

    if (filter.filename) params = params.set('filename', filter.filename);
    if (filter.assetType && filter.assetType !== ('ALL' as any)) params = params.set('assetType', filter.assetType);
    if (filter.contentType) params = params.set('contentType', filter.contentType);
    if (filter.tags) params = params.set('tags', filter.tags);
    if (filter.status && filter.status !== ('ALL' as any)) params = params.set('status', filter.status);
    if (filter.storageProvider) params = params.set('storageProvider', filter.storageProvider);
    if (filter.sort) params = params.set('sort', filter.sort);
    if (filter.order) params = params.set('order', filter.order);

    return this.http
      .get<ApiResponse<PaginatedAssetResponse>>(`${this.baseUrl}/search`, { params })
      .pipe(map((res) => res.data));
  }

  /**
   * Update user-supplied metadata on an existing asset.
   */
  updateMetadata(id: string, update: AssetMetadataUpdate): Observable<AssetResponse> {
    return this.http
      .put<ApiResponse<AssetResponse>>(`${this.baseUrl}/${id}/metadata`, update)
      .pipe(map((res) => res.data));
  }

  /**
   * Soft-delete an asset.
   */
  deleteAsset(id: string): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`${this.baseUrl}/${id}`)
      .pipe(map(() => undefined));
  }

  /**
   * Archive an active asset.
   */
  archiveAsset(id: string): Observable<AssetResponse> {
    return this.http
      .put<ApiResponse<AssetResponse>>(`${this.baseUrl}/${id}/archive`, {})
      .pipe(map((res) => res.data));
  }

  /**
   * Restore an archived asset back to ACTIVE.
   */
  restoreAsset(id: string): Observable<AssetResponse> {
    return this.http
      .put<ApiResponse<AssetResponse>>(`${this.baseUrl}/${id}/restore`, {})
      .pipe(map((res) => res.data));
  }

  /**
   * Get direct download URL for an asset.
   */
  getDownloadUrl(id: string): string {
    return `${this.baseUrl}/${id}/download`;
  }

  /**
   * Get public or CDN streaming URL for an asset.
   */
  getPublicUrl(id: string): Observable<string> {
    return this.http
      .get<ApiResponse<string>>(`${this.baseUrl}/${id}/url`)
      .pipe(map((res) => res.data));
  }

  /**
   * Format bytes to human readable string (KB, MB, GB).
   */
  formatFileSize(bytes?: number): string {
    if (!bytes && bytes !== 0) return '0 B';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
  }

  /**
   * Format seconds to mm:ss or hh:mm:ss.
   */
  formatDuration(seconds?: number): string {
    if (!seconds && seconds !== 0) return '';
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = Math.round(seconds % 60);
    if (h > 0) return `${h}h ${m}m ${s}s`;
    return m > 0 ? `${m}m ${s}s` : `${s}s`;
  }
}

export type AssetUploadPayload = AssetResponse;
