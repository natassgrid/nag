/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type TranslationStatus = 'DRAFT' | 'APPROVED' | 'STALE';

export interface TranslatedOptionDto {
  id: string;
  text: string;
}

export interface TranslationRequest {
  questionId: string;
  languageCode: string;
  translatorId: string;
  translatedContent: string;
  translatedOptions?: TranslatedOptionDto[];
  translatedExplanation?: string;
}

export interface TranslationReviewRequest {
  reviewerId: string;
  comments: string;
}

export interface TranslationResponse {
  translationId: string;
  questionId: string;
  languageCode: string;
  translatedContent: string;
  translatedOptions?: TranslatedOptionDto[];
  translatedExplanation?: string;
  sourceVersion: number;
  status: TranslationStatus;
  translatorId: string;
  reviewerId?: string;
  reviewComments?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface AutoTranslateResponse {
  questionId: string;
  languageCode: string;
  targetLangIndicTrans: string;
  translatedContent: string;
  translatedOptions?: TranslatedOptionDto[];
  translatedExplanation?: string;
  model?: string;
}

export interface SupportedLanguage {
  code: string;
  name: string;
  nativeName: string;
  script: string;
}

export const SUPPORTED_LANGUAGES: SupportedLanguage[] = [
  { code: 'hi', name: 'Hindi', nativeName: 'हिन्दी', script: 'Devanagari' },
  { code: 'bn', name: 'Bengali', nativeName: 'বাংলা', script: 'Bengali' },
  { code: 'te', name: 'Telugu', nativeName: 'తెలుగు', script: 'Telugu' },
  { code: 'mr', name: 'Marathi', nativeName: 'मराठी', script: 'Devanagari' },
  { code: 'ta', name: 'Tamil', nativeName: 'தமிழ்', script: 'Tamil' },
  { code: 'ur', name: 'Urdu', nativeName: 'اردو', script: 'Perso-Arabic' },
  { code: 'gu', name: 'Gujarati', nativeName: 'ગુજરાતી', script: 'Gujarati' },
  { code: 'kn', name: 'Kannada', nativeName: 'ಕನ್ನಡ', script: 'Kannada' },
  { code: 'ml', name: 'Malayalam', nativeName: 'മലയാളം', script: 'Malayalam' },
  { code: 'or', name: 'Odia', nativeName: 'ଓଡ଼ିଆ', script: 'Odia' },
  { code: 'pa', name: 'Punjabi', nativeName: 'ਪੰਜਾਬੀ', script: 'Gurmukhi' },
  { code: 'as', name: 'Assamese', nativeName: 'অসমীয়া', script: 'Bengali' },
  { code: 'mai', name: 'Maithili', nativeName: 'मैथिली', script: 'Devanagari' },
  { code: 'sa', name: 'Sanskrit', nativeName: 'संस्कृतम्', script: 'Devanagari' },
  { code: 'sd', name: 'Sindhi', nativeName: 'سنڌي', script: 'Perso-Arabic' },
  { code: 'ne', name: 'Nepali', nativeName: 'नेपाली', script: 'Devanagari' },
  { code: 'kok', name: 'Konkani', nativeName: 'कोंकणी', script: 'Devanagari' },
  { code: 'doi', name: 'Dogri', nativeName: 'डोगरी', script: 'Devanagari' },
  { code: 'mni', name: 'Manipuri', nativeName: 'মৈতৈলোন্', script: 'Bengali' },
  { code: 'sat', name: 'Santali', nativeName: 'ᱥᱟᱱᱛᱟᱲᱤ', script: 'Ol Chiki' },
  { code: 'bo', name: 'Bodo', nativeName: 'बड़ो', script: 'Devanagari' },
  { code: 'kas', name: 'Kashmiri', nativeName: 'कॉशुर', script: 'Perso-Arabic' }
];

@Injectable({ providedIn: 'root' })
export class TranslationService {
  private readonly baseUrl = '/api/v1/translations';

  constructor(private http: HttpClient) {}

  getLanguage(code: string): SupportedLanguage | undefined {
    return SUPPORTED_LANGUAGES.find(l => l.code === code || l.code.toLowerCase() === (code || '').toLowerCase());
  }

  /**
   * List all translations for a question across all languages and statuses.
   */
  listTranslationsForQuestion(questionId: string): Observable<TranslationResponse[]> {
    return this.http.get<TranslationResponse[]>(`${this.baseUrl}/question/${questionId}`);
  }

  /**
   * Get approved translation for a question and language code.
   */
  getApprovedTranslation(questionId: string, lang: string): Observable<TranslationResponse> {
    return this.http.get<TranslationResponse>(`${this.baseUrl}/question/${questionId}/language/${lang}`);
  }

  /**
   * Auto-translate a question into target language using IndicTrans2 AI model.
   */
  autoTranslateQuestion(questionId: string, languageCode: string): Observable<AutoTranslateResponse> {
    return this.http.post<AutoTranslateResponse>(`${this.baseUrl}/question/${questionId}/auto-translate/${languageCode}`, {});
  }

  /**
   * Submit a new translation for a question.
   */
  submitTranslation(request: TranslationRequest): Observable<{ translationId: string; status: string; message: string }> {
    return this.http.post<{ translationId: string; status: string; message: string }>(this.baseUrl, request);
  }

  /**
   * Resubmit a rejected translation.
   */
  resubmitTranslation(id: string, request: TranslationRequest): Observable<{ translationId: string; status: string; message: string }> {
    return this.http.put<{ translationId: string; status: string; message: string }>(`${this.baseUrl}/${id}`, request);
  }

  /**
   * Approve a translation.
   */
  approveTranslation(id: string, reviewerId: string): Observable<{ translationId: string; status: string; message: string }> {
    return this.http.post<{ translationId: string; status: string; message: string }>(`${this.baseUrl}/${id}/approve`, { reviewerId });
  }

  /**
   * Reject a translation with mandatory reviewer comments.
   */
  rejectTranslation(id: string, reviewerId: string, comments: string): Observable<{ translationId: string; status: string; message: string }> {
    const body: TranslationReviewRequest = { reviewerId, comments };
    return this.http.post<{ translationId: string; status: string; message: string }>(`${this.baseUrl}/${id}/reject`, body);
  }
}
