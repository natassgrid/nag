import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MathRendererComponent } from '../../../../math-renderer.component';
import {
  PlayerSubtopicMicroLesson,
  PlayerQuickCheckItem,
  QuizSelectionState,
  QuizSuccessStats,
  extractProblemStatementFromLesson,
  extractFinalAnswerFromLesson,
} from '../../../micro-lesson-player.model';

@Component({
  selector: 'nag-quiz-slide, app-quiz-slide',
  standalone: true,
  imports: [CommonModule, MathRendererComponent],
  templateUrl: './quiz-slide.component.html',
  styleUrl: './quiz-slide.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuizSlideComponent {
  readonly lesson = input<PlayerSubtopicMicroLesson | null>(null);
  readonly quizSelections = input<Record<number, QuizSelectionState>>({});

  readonly selectQuizOption = output<{ questionIndex: number; option: string }>();
  readonly resetQuiz = output<void>();

  readonly normalizedQuizItems = computed<PlayerQuickCheckItem[]>(() => {
    const items = this.lesson()?.content?.quickCheckItems || [];
    if (items.length > 0) {
      return items.map((item) => {
        const ans = (item.correctAnswer || item.answer || 'Verified result').trim();
        const opts =
          Array.isArray(item.options) && item.options.length >= 2
            ? item.options
            : [
                ans,
                'Zero under standard boundary conditions',
                'Inversely proportional to the input variable',
                'Cannot be determined from given constraints',
              ];
        return { ...item, options: opts, correctAnswer: ans, answer: ans };
      });
    }
    const fallbackAns =
      extractFinalAnswerFromLesson(this.lesson()) || 'Verified by governing principle';
    return [
      {
        question:
          extractProblemStatementFromLesson(this.lesson()) ||
          `Which statement best characterizes ${this.lesson()?.subtopicName || 'this subtopic'}?`,
        options: [
          fallbackAns,
          'Applies only when external forces vanish',
          'Independent of initial boundary conditions',
          'None of the above',
        ],
        correctAnswer: fallbackAns,
        answer: fallbackAns,
        explanation: this.lesson()?.content?.coreConcept || '',
      },
    ];
  });

  readonly quizStats = computed<QuizSuccessStats>(() => {
    const items = this.normalizedQuizItems();
    const selections = this.quizSelections() || {};
    const totalQuestions = items.length;
    const answeredEntries = Object.values(selections);
    const answeredCount = answeredEntries.length;
    const correctCount = answeredEntries.filter((s) => s.isCorrect).length;
    const accuracyPercent =
      answeredCount > 0 ? Math.round((correctCount / answeredCount) * 100) : 0;
    const isAllAnswered = totalQuestions > 0 && answeredCount >= totalQuestions;
    const statusLabel =
      answeredCount === 0
        ? 'Select an option to check mastery'
        : accuracyPercent >= 80
        ? 'Mastery Verified!'
        : accuracyPercent >= 50
        ? 'Good Progress'
        : 'Review Concept & Retry';
    return {
      totalQuestions,
      answeredCount,
      correctCount,
      accuracyPercent,
      isAllAnswered,
      statusLabel,
    };
  });

  optionLetter(idx: number): string {
    return String.fromCharCode(65 + idx);
  }
}
