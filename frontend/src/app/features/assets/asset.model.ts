/**
 * Asset models matching the backend asset-service DTOs.
 */

export interface AssetResponse {
  id: string;
  originalFilename: string;
  contentType: string;
  extension: string;
  fileSize: number;
  sha256Hash: string;
  assetType: AssetType;
  status: AssetStatus;
  width?: number;
  height?: number;
  dpi?: number;
  orientation?: string;
  durationSeconds?: number;
  codec?: string;
  bitrate?: number;
  sampleRate?: number;
  channels?: number;
  frameRate?: number;
  title?: string;
  description?: string;
  altText?: string;
  tags?: string;
  language?: string;
  storageProvider: string;
  storageLocation: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  tenantId: string;
}

export type AssetType = 'IMAGE' | 'AUDIO' | 'VIDEO' | 'DOCUMENT';
export type AssetStatus = 'ACTIVE' | 'ARCHIVED' | 'DELETED';

export interface AssetMetadataUpdate {
  title?: string;
  description?: string;
  altText?: string;
  tags?: string;
  language?: string;
}

export interface AssetSearchParams {
  filename?: string;
  assetType?: AssetType;
  contentType?: string;
  tags?: string;
  status?: AssetStatus;
  storageProvider?: string;
  page?: number;
  size?: number;
}
