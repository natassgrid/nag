// src/components/LanguageSelector.tsx
// Pre-exam language selection modal shown before session initialisation begins.
// Candidate picks their primary examination medium from the languages available
// for the assessment package.  English is always listed first (master reference).
// The modal cannot be dismissed without making a selection.

import React, { useState } from 'react';
import { Globe, ChevronRight, Info } from 'lucide-react';
import type { ExamLanguage } from '../types/api';

// ── All 22 scheduled Indian languages + English ──────────────────────────────
// Shown as fallback when the delivery service does not return availableLanguages.
export const ALL_EXAM_LANGUAGES: ExamLanguage[] = [
  { code: 'en', name: 'English',    nativeName: 'English' },
  { code: 'hi', name: 'Hindi',      nativeName: 'हिन्दी' },
  { code: 'ta', name: 'Tamil',      nativeName: 'தமிழ்' },
  { code: 'te', name: 'Telugu',     nativeName: 'తెలుగు' },
  { code: 'mr', name: 'Marathi',    nativeName: 'मराठी' },
  { code: 'bn', name: 'Bengali',    nativeName: 'বাংলা' },
  { code: 'gu', name: 'Gujarati',   nativeName: 'ગુજરાતી' },
  { code: 'kn', name: 'Kannada',    nativeName: 'ಕನ್ನಡ' },
  { code: 'ml', name: 'Malayalam',  nativeName: 'മലയാളം' },
  { code: 'pa', name: 'Punjabi',    nativeName: 'ਪੰਜਾਬੀ' },
  { code: 'or', name: 'Odia',       nativeName: 'ଓଡ଼ିଆ' },
  { code: 'as', name: 'Assamese',   nativeName: 'অসমীয়া' },
  { code: 'ur', name: 'Urdu',       nativeName: 'اردو', rtl: true },
  { code: 'ks', name: 'Kashmiri',   nativeName: 'کٲشُر', rtl: true },
  { code: 'sd', name: 'Sindhi',     nativeName: 'سنڌي', rtl: true },
  { code: 'ne', name: 'Nepali',     nativeName: 'नेपाली' },
  { code: 'sa', name: 'Sanskrit',   nativeName: 'संस्कृतम्' },
  { code: 'mai', name: 'Maithili',  nativeName: 'मैथिली' },
  { code: 'bho', name: 'Bhojpuri',  nativeName: 'भोजपुरी' },
  { code: 'mni', name: 'Manipuri',  nativeName: 'মৈতৈলোন্' },
  { code: 'kok', name: 'Konkani',   nativeName: 'कोंकणी' },
  { code: 'doi', name: 'Dogri',     nativeName: 'डोगरी' },
  { code: 'sat', name: 'Santali',   nativeName: 'ᱥᱟᱱᱛᱟᱲᱤ' },
  { code: 'bpy', name: 'Bodo',      nativeName: 'बड़ो' },
];

interface LanguageSelectorProps {
  /** Languages enabled for this assessment. If empty, ALL_EXAM_LANGUAGES is shown. */
  availableLanguages: ExamLanguage[];
  /** Language pre-selected during registration (from admit card / application). */
  defaultLanguageCode?: string;
  examTitle: string;
  onConfirm: (languageCode: string) => void;
}

export const LanguageSelector: React.FC<LanguageSelectorProps> = ({
  availableLanguages,
  defaultLanguageCode = 'en',
  examTitle,
  onConfirm,
}) => {
  const languages = availableLanguages.length > 0 ? availableLanguages : ALL_EXAM_LANGUAGES;
  const [selected, setSelected] = useState<string>(defaultLanguageCode);

  const selectedLang = languages.find((l) => l.code === selected) ?? languages[0];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/80 backdrop-blur-sm p-4 animate-in fade-in">
      <div className="w-full max-w-lg rounded-2xl bg-white shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="bg-teal-800 px-6 py-4">
          <div className="flex items-center gap-2 text-teal-200 text-xs font-semibold uppercase tracking-widest">
            <Globe className="h-4 w-4" />
            <span>Examination Medium Selection / परीक्षा माध्यम चयन</span>
          </div>
          <h2 className="mt-1 text-base font-bold text-white leading-snug line-clamp-2">
            {examTitle}
          </h2>
        </div>

        {/* Instruction notice */}
        <div className="flex items-start gap-2.5 bg-amber-50 border-b border-amber-200 px-5 py-3">
          <Info className="h-4 w-4 text-amber-600 shrink-0 mt-0.5" />
          <p className="text-xs text-amber-900 leading-relaxed">
            Select your preferred examination medium. You may switch between{' '}
            <strong>English</strong> and your chosen language on any question during the exam.
            In case of any discrepancy, the <strong>English version shall prevail</strong>.
          </p>
        </div>

        {/* Language grid */}
        <div className="px-5 py-4 max-h-72 overflow-y-auto">
          <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
            {languages.map((lang) => {
              const isSelected = selected === lang.code;
              return (
                <button
                  key={lang.code}
                  onClick={() => setSelected(lang.code)}
                  className={`flex flex-col items-start rounded-xl border-2 px-3 py-2.5 text-left transition ${
                    isSelected
                      ? 'border-teal-600 bg-teal-50 shadow-sm'
                      : 'border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50'
                  }`}
                >
                  <span
                    className={`text-sm font-bold leading-tight ${lang.rtl ? 'text-right w-full' : ''} ${
                      isSelected ? 'text-teal-800' : 'text-slate-800'
                    }`}
                    dir={lang.rtl ? 'rtl' : 'ltr'}
                  >
                    {lang.nativeName}
                  </span>
                  <span className="text-[11px] font-medium text-slate-500 mt-0.5">
                    {lang.name}
                  </span>
                  {isSelected && (
                    <span className="mt-1 rounded-full bg-teal-600 px-1.5 py-0.5 text-[10px] font-bold text-white">
                      Selected
                    </span>
                  )}
                </button>
              );
            })}
          </div>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between border-t border-slate-200 px-5 py-4 bg-slate-50">
          <div className="text-xs text-slate-600">
            <span>Chosen medium: </span>
            <span className="font-bold text-slate-900">
              {selectedLang.name} ({selectedLang.nativeName})
            </span>
          </div>
          <button
            onClick={() => onConfirm(selected)}
            className="inline-flex items-center gap-1.5 rounded-xl bg-teal-700 px-5 py-2.5 text-xs font-bold text-white hover:bg-teal-800 transition shadow-sm"
          >
            <span>Proceed to Exam</span>
            <ChevronRight className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  );
};

export default LanguageSelector;
