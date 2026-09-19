// src/components/PassagePanel.tsx
// Passage / Stimulus Display Panel for Paragraph-Based Comprehension & Case Study Questions
// Supports KaTeX math rendering, table markdown, and quick jump navigation across sub-questions.

import React from 'react';
import { BookOpen, FileText, ListOrdered } from 'lucide-react';
import { MathRenderer } from './MathRenderer';
import type { QuestionDto } from '../types/api';

interface PassagePanelProps {
  passageId: string;
  passageContent: string;
  allQuestions: QuestionDto[];
  currentQuestionIndex: number;
  onNavigateToQuestion: (index: number) => void;
  fontSize?: 'normal' | 'large' | 'xl';
}

export const PassagePanel: React.FC<PassagePanelProps> = ({
  passageId,
  passageContent,
  allQuestions,
  currentQuestionIndex,
  onNavigateToQuestion,
  fontSize = 'normal',
}) => {
  // Find all questions linked to this passage
  const subQuestions = allQuestions
    .map((q, idx) => ({ question: q, originalIndex: idx }))
    .filter((item) => item.question.passageId === passageId);

  const firstSubQNum = subQuestions.length > 0 ? subQuestions[0].originalIndex + 1 : currentQuestionIndex + 1;
  const lastSubQNum = subQuestions.length > 0 ? subQuestions[subQuestions.length - 1].originalIndex + 1 : currentQuestionIndex + 1;

  const fontClass =
    fontSize === 'xl' ? 'text-lg leading-relaxed' : fontSize === 'large' ? 'text-base leading-relaxed' : 'text-sm leading-relaxed';

  return (
    <div className="flex h-full flex-col bg-slate-50/80 border-r border-slate-200 overflow-hidden">
      {/* Passage Header & Group Progress Strip */}
      <div className="border-b border-slate-200 bg-white px-4 py-3 shadow-2xs">
        <div className="flex items-center justify-between gap-2 mb-2">
          <div className="flex items-center gap-2">
            <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-indigo-100 text-indigo-700">
              <BookOpen className="h-4 w-4" />
            </span>
            <div>
              <h3 className="text-xs font-bold uppercase tracking-wider text-slate-700">
                Comprehension Passage
              </h3>
              <p className="text-[11px] text-slate-500 font-medium">
                Questions {firstSubQNum} – {lastSubQNum} relate to this stimulus
              </p>
            </div>
          </div>
          <span className="inline-flex items-center gap-1 rounded-full bg-indigo-50 border border-indigo-200 px-2.5 py-0.5 text-[11px] font-bold text-indigo-700">
            <FileText className="h-3 w-3" />
            {subQuestions.length} Questions
          </span>
        </div>

        {/* Sub-question Quick-Navigation Pills */}
        <div className="flex items-center gap-1.5 overflow-x-auto pt-1">
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mr-1 flex items-center gap-0.5">
            <ListOrdered className="h-3 w-3" /> Jump:
          </span>
          {subQuestions.map(({ originalIndex }) => {
            const isCurrent = originalIndex === currentQuestionIndex;
            return (
              <button
                key={originalIndex}
                onClick={() => onNavigateToQuestion(originalIndex)}
                className={`h-6 min-w-[24px] px-1.5 rounded text-xs font-bold transition flex items-center justify-center ${
                  isCurrent
                    ? 'bg-indigo-600 text-white shadow-xs scale-105'
                    : 'bg-slate-100 text-slate-700 hover:bg-indigo-50 hover:text-indigo-600 border border-slate-200'
                }`}
                title={`Jump to Question ${originalIndex + 1}`}
              >
                Q{originalIndex + 1}
              </button>
            );
          })}
        </div>
      </div>

      {/* Scrollable Stimulus Content */}
      <div className="flex-1 overflow-y-auto p-5">
        <div className={`prose prose-slate max-w-none text-slate-800 ${fontClass}`}>
          <MathRenderer content={passageContent} />
        </div>
      </div>
    </div>
  );
};
export default PassagePanel;
