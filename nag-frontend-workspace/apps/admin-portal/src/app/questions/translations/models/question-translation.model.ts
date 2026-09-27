export interface QuestionTranslationFilter {
  searchQuery: string;
  selectedSubject: string;
}

export interface QuestionTranslationDraft {
  questionId: string;
  languageCode: string;
  translatedContent: string;
  translatedExplanation: string;
  translatedOptions: { id: string; text: string }[];
  status: 'DRAFT' | 'APPROVED';
}

export interface BatchJobTriggerRequest {
  sourceLanguage: string;
  targetLanguage: string;
  subject?: string;
  overwriteExisting: boolean;
}
