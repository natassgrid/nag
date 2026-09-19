// src/components/GroupProgressStrip.tsx
// Renders group indicator dots (● ○ ○) and quick jump badges for sub-questions within a passage group.

import React from 'react';
import type { QuestionDto } from '../types/api';

interface GroupProgressStripProps {
  passageId: string;
  allQuestions: QuestionDto[];
  currentQuestionIndex: number;
  onNavigateToQuestion: (index: number) => void;
}

export const GroupProgressStrip: React.FC<GroupProgressStripProps> = ({
  passageId,
  allQuestions,
  currentQuestionIndex,
  onNavigateToQuestion,
}) => {
  const groupQuestions = allQuestions
    .map((q, idx) => ({ q, idx }))
    .filter(({ q }) => q.passageId === passageId)
    .sort((a, b) => (a.q.passageOrderIndex ?? a.idx) - (b.q.passageOrderIndex ?? b.idx));

  if (groupQuestions.length <= 1) return null;

  return (
    <div className="flex items-center gap-2 py-1 px-3 bg-teal-50/80 rounded-lg border border-teal-200/70 text-xs">
      <span className="font-bold text-teal-900 tracking-wide">
        Passage Group:
      </span>
      <div className="flex items-center gap-1.5">
        {groupQuestions.map(({ q, idx }, groupIdx) => {
          const isCurrent = idx === currentQuestionIndex;
          const orderNum = q.passageOrderIndex || groupIdx + 1;
          return (
            <button
              key={q.id}
              onClick={() => onNavigateToQuestion(idx)}
              className={`h-6 px-2 rounded-md font-bold text-[11px] transition flex items-center gap-1 ${
                isCurrent
                  ? 'bg-teal-700 text-white shadow-xs scale-105'
                  : 'bg-white text-teal-800 border border-teal-300 hover:bg-teal-100'
              }`}
              title={`Jump to Sub-Question ${orderNum} (Overall Q${idx + 1})`}
            >
              <span>{isCurrent ? '●' : '○'}</span>
              <span>Q{idx + 1}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
};
