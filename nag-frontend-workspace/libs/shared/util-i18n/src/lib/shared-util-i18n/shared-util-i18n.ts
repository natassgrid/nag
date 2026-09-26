import { Injectable, Pipe, PipeTransform, computed, inject, signal } from '@angular/core';

export type SupportedLanguage =
  | 'en'
  | 'hi'
  | 'bn'
  | 'ta'
  | 'te'
  | 'mr'
  | 'gu'
  | 'kn'
  | 'ml'
  | 'pa'
  | 'or'
  | 'as'
  | 'ur';

export interface LanguageOption {
  code: SupportedLanguage;
  label: string;
  nativeLabel: string;
}

export const SUPPORTED_LANGUAGES: LanguageOption[] = [
  { code: 'en', label: 'English', nativeLabel: 'English' },
  { code: 'hi', label: 'Hindi', nativeLabel: 'हिन्दी' },
  { code: 'bn', label: 'Bengali', nativeLabel: 'বাংলা' },
  { code: 'ta', label: 'Tamil', nativeLabel: 'தமிழ்' },
  { code: 'te', label: 'Telugu', nativeLabel: 'తెలుగు' },
  { code: 'mr', label: 'Marathi', nativeLabel: 'मराठी' },
  { code: 'gu', label: 'Gujarati', nativeLabel: 'ગુજરાતી' },
  { code: 'kn', label: 'Kannada', nativeLabel: 'ಕನ್ನಡ' },
  { code: 'ml', label: 'Malayalam', nativeLabel: 'മലയാളം' },
  { code: 'pa', label: 'Punjabi', nativeLabel: 'ਪੰਜਾਬੀ' },
  { code: 'or', label: 'Odia', nativeLabel: 'ଓଡ଼ିଆ' },
  { code: 'as', label: 'Assamese', nativeLabel: 'অসমীয়া' },
  { code: 'ur', label: 'Urdu', nativeLabel: 'اردو' },
];

export const DEFAULT_TRANSLATIONS: Record<SupportedLanguage, Record<string, string>> = {
  en: {
    'common.save': 'Save',
    'common.cancel': 'Cancel',
    'common.delete': 'Delete',
    'common.edit': 'Edit',
    'common.search': 'Search...',
    'common.loading': 'Loading...',
    'common.status': 'Status',
    'common.actions': 'Actions',
    'common.confirm': 'Confirm',
    'exam.title': 'National Assessment Grid',
    'exam.candidate_name': 'Candidate Name',
    'exam.roll_number': 'Roll Number',
    'exam.time_remaining': 'Time Remaining',
    'exam.question_palette': 'Question Palette',
    'exam.next': 'Next Question',
    'exam.previous': 'Previous Question',
    'exam.mark_review': 'Mark for Review',
    'exam.clear_response': 'Clear Response',
    'exam.submit_exam': 'Submit Examination',
  },
  hi: {
    'common.save': 'सहेजें',
    'common.cancel': 'रद्द करें',
    'common.delete': 'हटाएं',
    'common.edit': 'संपादित करें',
    'common.search': 'खोजें...',
    'common.loading': 'लोड हो रहा है...',
    'common.status': 'स्थिति',
    'common.actions': 'कार्रवाई',
    'common.confirm': 'पुष्टि करें',
    'exam.title': 'राष्ट्रीय मूल्यांकन ग्रिड',
    'exam.candidate_name': 'परीक्षार्थी का नाम',
    'exam.roll_number': 'अनुक्रमांक',
    'exam.time_remaining': 'शेष समय',
    'exam.question_palette': 'प्रश्न तालिका',
    'exam.next': 'अगला प्रश्न',
    'exam.previous': 'पिछला प्रश्न',
    'exam.mark_review': 'समीक्षा के लिए चिह्नित करें',
    'exam.clear_response': 'उत्तर हटाएं',
    'exam.submit_exam': 'परीक्षा जमा करें',
  },
  bn: {
    'common.save': 'সংরক্ষণ',
    'common.cancel': 'বাতিল',
    'common.search': 'অনুসন্ধান...',
  },
  ta: {
    'common.save': 'சேமி',
    'common.cancel': 'ரத்து செய்',
    'common.search': 'தேடு...',
  },
  te: {
    'common.save': 'భద్రపరుచు',
    'common.cancel': 'రద్దు చేయి',
    'common.search': 'శోధించండి...',
  },
  mr: {
    'common.save': 'जतन करा',
    'common.cancel': 'रद्द करा',
    'common.search': 'शोधा...',
  },
  gu: {
    'common.save': 'સાચવો',
    'common.cancel': 'રદ કરો',
    'common.search': 'શોધો...',
  },
  kn: {
    'common.save': 'ಉಳಿಸಿ',
    'common.cancel': 'ರದ್ದುಮಾಡಿ',
    'common.search': 'ಹುಡುಕಿ...',
  },
  ml: {
    'common.save': 'സംരക്ഷിക്കുക',
    'common.cancel': 'റദ്ദാക്കുക',
    'common.search': 'തിരയുക...',
  },
  pa: {
    'common.save': 'ਸੰਭਾਲੋ',
    'common.cancel': 'ਰੱਦ ਕਰੋ',
    'common.search': 'ਖੋਜੋ...',
  },
  or: {
    'common.save': 'ସାଇତନ୍ତୁ',
    'common.cancel': 'ବାତିଲ୍ କରନ୍ତୁ',
    'common.search': 'ଖୋଜନ୍ତୁ...',
  },
  as: {
    'common.save': 'সংৰক্ষণ কৰক',
    'common.cancel': 'বাতিল কৰক',
    'common.search': 'সন্ধান কৰক...',
  },
  ur: {
    'common.save': 'محفوظ کریں',
    'common.cancel': 'منسوخ کریں',
    'common.search': 'تلاش کریں...',
  },
};

@Injectable({
  providedIn: 'root',
})
export class I18nService {
  private readonly STORAGE_KEY = 'nag_i18n_lang';
  readonly currentLanguage = signal<SupportedLanguage>(this.getInitialLanguage());

  private translations = signal<Record<SupportedLanguage, Record<string, string>>>(
    DEFAULT_TRANSLATIONS
  );

  setLanguage(lang: SupportedLanguage): void {
    this.currentLanguage.set(lang);
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(this.STORAGE_KEY, lang);
    }
  }

  translate(key: string, params?: Record<string, string>): string {
    const lang = this.currentLanguage();
    const dictionary = this.translations()[lang] || this.translations()['en'];
    let text = dictionary[key] || this.translations()['en'][key] || key;

    if (params) {
      Object.entries(params).forEach(([pKey, pVal]) => {
        text = text.replace(new RegExp(`{{\\s*${pKey}\\s*}}`, 'g'), pVal);
      });
    }

    return text;
  }

  translateSignal(key: string, params?: Record<string, string>) {
    return computed(() => {
      // Recomputes whenever currentLanguage or translations update
      return this.translate(key, params);
    });
  }

  private getInitialLanguage(): SupportedLanguage {
    if (typeof localStorage !== 'undefined') {
      const stored = localStorage.getItem(this.STORAGE_KEY) as SupportedLanguage;
      if (stored && SUPPORTED_LANGUAGES.some((l) => l.code === stored)) {
        return stored;
      }
    }
    return 'en';
  }
}

@Pipe({
  name: 'nagTranslate',
  standalone: true,
  pure: false,
})
export class NagTranslatePipe implements PipeTransform {
  private readonly i18n = inject(I18nService);

  transform(key: string, params?: Record<string, string>): string {
    return this.i18n.translate(key, params);
  }
}
