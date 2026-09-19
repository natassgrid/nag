// src/components/ParagraphQuestionLayout.tsx
// Split-pane container for Comprehension / Paragraph questions (40/60 desktop split, responsive stacked on mobile).

import React from 'react';
import { PassagePanel } from './PassagePanel';
import type { QuestionDto } from '../types/api';

interface ParagraphQuestionLayoutProps {
  currentQuestion: QuestionDto;
  questions: QuestionDto[];
  currentIndex: number;
  onNavigateToQuestion: (index: number) => void;
  fontSize: 'normal' | 'large';
  children: React.ReactNode;
}

export const ParagraphQuestionLayout: React.FC<ParagraphQuestionLayoutProps> = ({
  currentQuestion,
  questions,
  currentIndex,
  onNavigateToQuestion,
  fontSize,
  children,
}) => {
  if (!currentQuestion.passageId || !currentQuestion.passageContent) {
    return <>{children}</>;
  }

  return (
    <div className="flex flex-1 flex-col lg:flex-row overflow-hidden w-full h-full">
      {/* Left Pane: Stimulus / Passage (40% desktop min-width, independently scrollable) */}
      <div className="w-full lg:w-[42%] flex flex-col border-b lg:border-b-0 lg:border-r border-slate-200 bg-slate-50/50">
        <PassagePanel
          passageId={currentQuestion.passageId}
          passageContent={currentQuestion.passageContent}
          allQuestions={questions}
          currentQuestionIndex={currentIndex}
          onNavigateToQuestion={onNavigateToQuestion}
          fontSize={fontSize}
        />
      </div>

      {/* Right Pane: Sub-question stem + options + actions */}
      <div className="flex-1 flex flex-col overflow-y-auto bg-white p-6">
        {children}
      </div>
    </div>
  );
};
