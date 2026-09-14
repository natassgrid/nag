// src/components/ExamLanguageBanner.tsx
// Persistent regulatory disclaimer banner shown whenever a candidate is viewing
// a question in a regional language.  Complies with NTA/SSC/RRB standard that
// "in case of any discrepancy in translation, the English version shall prevail."

import React from 'react';
import { AlertTriangle } from 'lucide-react';
import type { ExamLanguage } from '../types/api';

interface ExamLanguageBannerProps {
  activeLanguage: ExamLanguage;
  onSwitchToEnglish: () => void;
}

export const ExamLanguageBanner: React.FC<ExamLanguageBannerProps> = ({
  activeLanguage,
  onSwitchToEnglish,
}) => {
  // Only show when viewing a regional language (not English)
  if (activeLanguage.code === 'en') return null;

  return (
    <div className="flex items-center justify-between gap-3 border-b border-amber-300 bg-amber-50 px-4 py-2">
      <div className="flex items-center gap-2">
        <AlertTriangle className="h-3.5 w-3.5 shrink-0 text-amber-600" />
        <p className="text-[11px] font-semibold text-amber-900 leading-tight">
          Viewing in{' '}
          <span className="font-black" dir={activeLanguage.rtl ? 'rtl' : 'ltr'}>
            {activeLanguage.nativeName}
          </span>{' '}
          ({activeLanguage.name}).&nbsp;
          <span className="font-bold">
            In case of any discrepancy in translation, the English version shall be deemed final
            and authentic.
          </span>
        </p>
      </div>
      <button
        onClick={onSwitchToEnglish}
        className="shrink-0 rounded-md border border-amber-400 bg-white px-2.5 py-1 text-[11px] font-bold text-amber-900 hover:bg-amber-100 transition"
      >
        View English
      </button>
    </div>
  );
};

export default ExamLanguageBanner;
