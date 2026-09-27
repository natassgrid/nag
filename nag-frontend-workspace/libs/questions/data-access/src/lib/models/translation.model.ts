export type TranslationStatus = 'DRAFT' | 'PENDING_REVIEW' | 'APPROVED' | 'PUBLISHED' | 'REJECTED' | 'STALE';

export interface IndicTranslationLanguage {
  code: string;
  name: string;
  nativeName: string;
  script: string;
}

export const INDIC_TRANSLATION_LANGUAGES: IndicTranslationLanguage[] = [
  { code: 'hi', name: 'Hindi', nativeName: 'हिन्दी', script: 'Devanagari' },
  { code: 'bn', name: 'Bengali', nativeName: 'বাংলা', script: 'Bengali' },
  { code: 'te', name: 'Telugu', nativeName: 'తెలుగు', script: 'Telugu' },
  { code: 'mr', name: 'Marathi', nativeName: 'मराठी', script: 'Devanagari' },
  { code: 'ta', name: 'Tamil', nativeName: 'தமிழ்', script: 'Tamil' },
  { code: 'gu', name: 'Gujarati', nativeName: 'ગુજરાતી', script: 'Gujarati' },
  { code: 'kn', name: 'Kannada', nativeName: 'ಕನ್ನಡ', script: 'Kannada' },
  { code: 'ml', name: 'Malayalam', nativeName: 'മലയാളം', script: 'Malayalam' },
  { code: 'or', name: 'Odia', nativeName: 'ଓଡ଼ିଆ', script: 'Odia' },
  { code: 'pa', name: 'Punjabi', nativeName: 'ਪੰਜਾਬੀ', script: 'Gurmukhi' },
  { code: 'as', name: 'Assamese', nativeName: 'অসমীয়া', script: 'Bengali' },
  { code: 'ur', name: 'Urdu', nativeName: 'اردو', script: 'Perso-Arabic' },
  { code: 'sa', name: 'Sanskrit', nativeName: 'संस्कृतम्', script: 'Devanagari' },
  { code: 'en', name: 'English', nativeName: 'English', script: 'Latin' },
];

/** @deprecated Use IndicTranslationLanguage */
export type SupportedLanguage = IndicTranslationLanguage;
/** @deprecated Use INDIC_TRANSLATION_LANGUAGES */
export const SUPPORTED_LANGUAGES = INDIC_TRANSLATION_LANGUAGES;

export interface TranslatedOptionDto {
  id: string;
  text: string;
  imageUrl?: string;
  imageAltText?: string;
}

export interface TranslationRequest {
  questionId: string;
  languageCode: string;
  translatorId?: string;
  translatedContent: string;
  translatedOptions?: TranslatedOptionDto[];
  translatedExplanation?: string;
}

export interface TranslationResponse {
  translationId?: string;
  id?: string;
  questionId: string;
  languageCode: string;
  translatedContent: string;
  translatedOptions?: TranslatedOptionDto[];
  translatedExplanation?: string;
  sourceVersion?: number;
  status: TranslationStatus;
  translatorId?: string;
  reviewerId?: string;
  reviewComments?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface AutoTranslateResponse {
  questionId: string;
  languageCode?: string;
  language?: string;
  targetLangIndicTrans?: string;
  translatedContent: string;
  translatedOptions?: TranslatedOptionDto[];
  translatedExplanation?: string;
  model?: string;
  confidenceScore?: number;
}

export interface BatchTranslationRequest {
  sourceLanguage?: string;
  targetLanguage?: string;
  targetStatus?: string;
  subject?: string;
  overwriteExisting?: boolean;
  batchSize?: number;
  throttleDelayMs?: number;
  maxConcurrency?: number;
}

export type BatchJobStatus = 'PENDING' | 'IN_PROGRESS' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';

export interface BatchTranslationJobResponse {
  id: string;
  jobId?: string;
  tenantId?: string;
  status: BatchJobStatus;
  sourceLanguage: string;
  targetLanguage: string;
  targetStatus?: string;
  subjectFilter?: string;
  overwriteExisting?: boolean;
  totalQuestions: number;
  processedQuestions: number;
  successfulQuestions: number;
  translatedCount?: number;
  failedQuestions: number;
  progressPercentage: number;
  failedQuestionIds?: string[];
  batchSize?: number;
  initiatedBy?: string;
  startedAt?: string;
  completedAt?: string;
  errorMessage?: string;
  createdAt?: string;
  updatedAt?: string;
}
