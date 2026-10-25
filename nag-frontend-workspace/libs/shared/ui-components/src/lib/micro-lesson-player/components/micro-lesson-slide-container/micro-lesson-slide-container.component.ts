import {
  Component,
  ChangeDetectionStrategy,
  HostListener,
  computed,
  input,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MathRendererComponent } from '../../../math-renderer.component';
import {
  PlayerSubtopicMicroLesson,
  PlayerMicroLessonStep,
  LessonSlide,
  PlayerTrapAlert,
  PlayerFormulaEntry,
  PlayerQuickCheckItem,
  QuizSelectionState,
  QuizSuccessStats,
  TutorAvatarState,
  extractStepsFromLesson,
  extractProblemStatementFromLesson,
  extractFinalAnswerFromLesson,
} from '../../micro-lesson-player.model';

@Component({
  selector: 'nag-micro-lesson-slide-container, app-micro-lesson-slide-container',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MathRendererComponent,
  ],
  templateUrl: './micro-lesson-slide-container.component.html',
  styleUrl: './micro-lesson-slide-container.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MicroLessonSlideContainerComponent {
  readonly lesson = input.required<PlayerSubtopicMicroLesson>();
  readonly slides = input.required<LessonSlide[]>();
  readonly currentSlideIndex = input<number>(0);
  readonly revealedStepCount = input<number>(0);
  readonly quizSelections = input<Record<number, QuizSelectionState>>({});
  readonly isPlaying = input<boolean>(false);
  readonly audioEnabled = input<boolean>(false);
  readonly tutorStageExpanded = input<boolean>(false);
  readonly tutorAvatarState = input<TutorAvatarState>('IDLE');
  readonly activeCaption = input<string>('');
  readonly formulaRequired = input<boolean>(true);
  readonly isFullscreen = input<boolean>(false);

  readonly nextSlide = output<void>();
  readonly prevSlide = output<void>();
  readonly goToSlide = output<number>();
  readonly revealNextStep = output<void>();
  readonly revealAllSteps = output<void>();
  readonly resetSteps = output<void>();
  readonly selectQuizOption = output<{ questionIndex: number; option: string }>();
  readonly resetQuiz = output<void>();
  readonly startAutoPlay = output<void>();
  readonly toggleFullscreen = output<void>();

  readonly activeFormulaIndex = signal<number>(0);
  private touchStartX = 0;

  readonly windowHeight = signal<number>(
    typeof window !== 'undefined' ? window.innerHeight : 800
  );

  @HostListener('window:resize')
  onResize(): void {
    if (typeof window !== 'undefined') {
      this.windowHeight.set(window.innerHeight);
    }
  }

  readonly calculatedCardHeight = computed<string>(() => {
    if (this.isFullscreen()) {
      return 'calc(100vh - 105px)';
    }
    const vh = this.windowHeight();
    const target = Math.max(340, Math.min(620, vh - 220));
    return `${target}px`;
  });

  readonly currentSlide = computed<LessonSlide>(() => {
    const list = this.slides() || [];
    return (
      list[this.currentSlideIndex()] ||
      list[0] || {
        id: 'fallback-intro',
        index: 0,
        type: 'INTRO',
        title: this.lesson()?.subtopicName || 'Micro-Lesson',
        subtitle: '',
        icon: 'school',
        durationSeconds: 6,
        narrationText: '',
      }
    );
  });

  readonly normalizedFormulas = computed<PlayerFormulaEntry[]>(() => {
    const sheet =
      this.lesson()?.content?.formulaCheatSheet ||
      (this.lesson() as any)?.formulaCheatSheet;

    if (Array.isArray(sheet)) {
      const valid = sheet
        .map((f: any, idx: number): PlayerFormulaEntry | null => {
          if (!f) return null;
          if (typeof f === 'string') {
            const trimmed = f.trim();
            const colonIdx = trimmed.indexOf(':');
            const dollarIdx = trimmed.indexOf('$');
            const slashIdx = trimmed.indexOf('\\');
            const hasColon = colonIdx > 0;
            const colonBeforeDollar = dollarIdx === -1 || colonIdx < dollarIdx;
            const colonBeforeSlash = slashIdx === -1 || colonIdx < slashIdx;
            let name = `Formula ${idx + 1}`;
            let latex = trimmed;
            if (hasColon && colonBeforeDollar && colonBeforeSlash) {
              const namePart = trimmed.substring(0, colonIdx).trim();
              const latexPart = trimmed.substring(colonIdx + 1).trim();
              if (namePart && !namePart.includes('$') && !namePart.includes('\\')) {
                name = namePart;
                latex = latexPart;
              }
            }
            return {
              name,
              latex,
              description: '',
              variables: [],
            };
          }
          return {
            name: f.name || f.title || f.namePart || `Formula ${idx + 1}`,
            latex: f.latex || f.formula || f.expression || f.value || '',
            description: f.description || f.note || f.usage || '',
            variables: f.variables || [],
          };
        })
        .filter(
          (f): f is PlayerFormulaEntry =>
            !!f && (f.latex.trim().length > 0 || f.name.trim().length > 0)
        );

      if (valid.length > 0) return valid;
    }
    if (typeof sheet === 'string' && sheet.trim()) {
      return [
        {
          name: 'Core Governing Formula',
          latex: sheet.trim(),
          description: 'Primary mathematical identity for this subtopic.',
        },
      ];
    }
    if (!this.formulaRequired()) {
      return [];
    }
    const takeaways = this.lesson()?.content?.keyTakeaways || [];
    return takeaways.slice(0, 2).map((t, i) => ({
      name: `Key Principle ${i + 1}`,
      latex: t,
      description: 'Core conceptual rule for exam application.',
    }));
  });

  readonly allSteps = computed<PlayerMicroLessonStep[]>(() =>
    extractStepsFromLesson(this.lesson())
  );

  readonly visibleSteps = computed<PlayerMicroLessonStep[]>(() => {
    return this.allSteps().slice(0, this.revealedStepCount());
  });

  readonly totalStepsCount = computed<number>(() => {
    return this.allSteps().length;
  });

  readonly problemStatementText = computed<string>(() =>
    extractProblemStatementFromLesson(this.lesson())
  );

  readonly finalAnswerText = computed<string>(() =>
    extractFinalAnswerFromLesson(this.lesson())
  );

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
      this.finalAnswerText() || 'Verified by governing principle';
    return [
      {
        question:
          this.problemStatementText() ||
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

  cleanStepExplanation(step: PlayerMicroLessonStep): string {
    if (!step || !step.explanation) return '';
    return (
      step.explanation
        .replace(/^\s*step\s*\d+\s*[:\-.]?\s*/i, '')
        .trim() || step.explanation
    );
  }

  hasDistinctStepTitle(step: PlayerMicroLessonStep): boolean {
    if (!step || !step.title) return false;
    const trimmed = step.title.trim();
    if (/^step\s*\d+\s*[:\-.]?$/i.test(trimmed)) {
      return false;
    }
    return true;
  }

  asTrapAlert(trap: unknown): PlayerTrapAlert | null {
    return typeof trap === 'object' && trap !== null && 'title' in trap
      ? (trap as PlayerTrapAlert)
      : null;
  }

  asString(val: unknown): string {
    return typeof val === 'string' ? val : '';
  }

  formatLatex(latex: string): string {
    let trimmed = (latex || '').trim();
    if (!trimmed) return '';
    // Strip redundant leading "Formula Name: " prefix if embedded in latex string
    const colonIdx = trimmed.indexOf(':');
    const dollarIdx = trimmed.indexOf('$');
    const slashIdx = trimmed.indexOf('\\');
    if (
      colonIdx > 0 &&
      (dollarIdx > colonIdx || (dollarIdx === -1 && slashIdx > colonIdx))
    ) {
      trimmed = trimmed.substring(colonIdx + 1).trim();
    }
    if (trimmed.includes('$$') || trimmed.includes('$')) return trimmed;
    return `$$${trimmed}$$`;
  }

  optionLetter(idx: number): string {
    return String.fromCharCode(65 + idx);
  }

  onTouchStart(event: TouchEvent): void {
    this.touchStartX = event.changedTouches?.[0]?.screenX || 0;
  }

  onTouchEnd(event: TouchEvent): void {
    const endX = event.changedTouches?.[0]?.screenX || 0;
    const deltaX = endX - this.touchStartX;
    if (Math.abs(deltaX) > 50) {
      if (deltaX < 0) this.nextSlide.emit();
      else this.prevSlide.emit();
    }
  }
}
