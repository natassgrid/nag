import {
  Component,
  ChangeDetectionStrategy,
  OnDestroy,
  computed,
  effect,
  input,
  signal,
  untracked,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import {
  MathRendererComponent,
  MicroLessonPlayerTopbarComponent,
  MicroLessonSlideContainerComponent,
  PlayerSubtopicMicroLesson,
  LessonSlide,
  QuizSelectionState,
  LessonPlaybackMode,
  TutorAvatarState,
  createSlidesFromLesson,
} from '@nag-frontend-workspace/shared-ui-components';
import {
  FormulaEntry,
  MicroLessonStep,
  QuickCheckItem,
  TrapAlert,
} from '../../../../models/micro-lesson.model';

@Component({
  selector: 'app-lesson-live-preview',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MathRendererComponent,
    MicroLessonPlayerTopbarComponent,
    MicroLessonSlideContainerComponent,
  ],
  templateUrl: './lesson-live-preview.component.html',
  styleUrl: './lesson-live-preview.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LessonLivePreviewComponent implements OnDestroy {
  readonly subjectName = input<string>('General');
  readonly topicName = input<string>('General');
  readonly subtopicName = input<string>('Subtopic');
  readonly targetExam = input<string>('JEE_MAIN');
  readonly lessonType = input<string>('CONCEPT');
  readonly difficultyLevel = input<string>('CORE');
  readonly estimatedMinutes = input<number>(5);
  readonly status = input<string>('APPROVED');
  readonly coreConcept = input<string>('');
  readonly trapAlert = input<TrapAlert>({
    title: '',
    description: '',
    commonMistake: '',
    correctApproach: '',
  });
  readonly formulas = input<FormulaEntry[]>([]);
  readonly formulaRequired = input<boolean>(true);
  readonly problemStatement = input<string>('');
  readonly steps = input<MicroLessonStep[]>([]);
  readonly finalAnswer = input<string>('');
  readonly keyTakeaways = input<string[]>([]);
  readonly quickCheckItems = input<QuickCheckItem[]>([]);

  // Interactive Slide Player State
  readonly previewMode = signal<'PLAYER' | 'SCROLL'>('PLAYER');
  readonly currentSlideIndex = signal<number>(0);
  readonly revealedStepCount = signal<number>(0);
  readonly quizSelections = signal<Record<number, QuizSelectionState>>({});
  readonly isPlaying = signal<boolean>(false);
  readonly playbackMode = signal<LessonPlaybackMode>('MANUAL');
  readonly playbackSpeed = signal<number>(1);
  readonly autoPlayProgress = signal<number>(0);
  readonly audioEnabled = signal<boolean>(false);
  readonly tutorStageExpanded = signal<boolean>(false);
  readonly tutorAvatarState = signal<TutorAvatarState>('IDLE');
  readonly activeCaption = signal<string>('');

  private autoPlayTimer: ReturnType<typeof setInterval> | null = null;

  readonly nonEmptyFormulas = computed(() =>
    (this.formulas() || []).filter(
      (f) =>
        f &&
        ((f.latex && f.latex.trim().length > 0) ||
          (f.description && f.description.trim().length > 0) ||
          (f.name && f.name.trim().length > 0 && !/^formula\s+\d+$/i.test(f.name.trim())))
    )
  );

  readonly previewFormulas = computed(() =>
    this.formulaRequired() ? this.formulas() || [] : this.nonEmptyFormulas()
  );

  readonly nonEmptyTakeaways = computed(() =>
    (this.keyTakeaways() || []).filter((t) => t && t.trim().length > 0)
  );

  readonly nonEmptyQuickChecks = computed(() =>
    (this.quickCheckItems() || []).filter(
      (q) => q && q.question && q.question.trim().length > 0
    )
  );

  readonly previewLesson = computed<PlayerSubtopicMicroLesson>(() => {
    const trap = this.trapAlert() || {
      title: '',
      description: '',
      commonMistake: '',
      correctApproach: '',
    };
    return {
      id: 'authoring-live-preview',
      subtopicId: 0,
      subtopicName: this.subtopicName() || 'Untitled Subtopic',
      topicName: this.topicName() || 'Topic',
      subjectName: this.subjectName() || 'Subject',
      targetExam: this.targetExam() || 'UNIVERSAL',
      lessonType: this.lessonType() || 'CONCEPT',
      approachName: this.lessonType() || 'CONCEPT',
      difficultyLevel: this.difficultyLevel() || 'CORE',
      estimatedMinutes: this.estimatedMinutes() || 5,
      status: this.status() || 'APPROVED',
      contentVersion: 1,
      content: {
        coreConcept:
          this.coreConcept() ||
          'Enter core concept explanation on the left to preview live KaTeX rendering...',
        trapAlert: {
          title: trap.title || 'Exam Trap & Common Pitfall',
          description: trap.description || 'Describe the common exam pitfall...',
          commonMistake: trap.commonMistake,
          correctApproach: trap.correctApproach,
        },
        formulaCheatSheet: this.previewFormulas().map((f, idx) => ({
          name: f.name || `Formula ${idx + 1}`,
          latex: f.latex || '',
          description: f.description || '',
          variables: f.variables || [],
        })),
        stepByStepBreakdown: {
          problemStatement: this.problemStatement(),
          questionStem: this.problemStatement(),
          steps: (this.steps() || []).map((s, idx) => ({
            stepNumber: s.stepNumber || idx + 1,
            title: s.title || `Step ${idx + 1}`,
            explanation: s.explanation || '',
            formula: s.formula,
            tip: s.tip,
          })),
          finalAnswer: this.finalAnswer(),
        },
        keyTakeaways: this.nonEmptyTakeaways(),
        quickCheckItems: this.nonEmptyQuickChecks().map((q) => {
          const ans = (q.correctAnswer || q.answer || 'Verified answer').trim();
          return {
            question: q.question,
            options: q.options && q.options.length >= 2 ? q.options : undefined,
            correctAnswer: ans,
            answer: ans,
            explanation: q.explanation,
          };
        }),
      },
    };
  });

  readonly slides = computed<LessonSlide[]>(() =>
    createSlidesFromLesson(this.previewLesson())
  );

  readonly totalSlides = computed<number>(() => this.slides().length || 5);

  constructor() {
    effect(() => {
      const playing = this.isPlaying();
      const speed = this.playbackSpeed();
      const slideIdx = this.currentSlideIndex();
      const slideList = this.slides();
      this.clearAutoPlayTimer();

      if (!playing || slideList.length === 0) {
        this.autoPlayProgress.set(0);
        if (!this.audioEnabled()) this.tutorAvatarState.set('IDLE');
        return;
      }

      const activeSlide = slideList[slideIdx];
      if (!activeSlide || activeSlide.type === 'QUIZ') {
        this.isPlaying.set(false);
        this.playbackMode.set('MANUAL');
        this.tutorAvatarState.set('CELEBRATING');
        return;
      }

      this.tutorAvatarState.set(activeSlide.mediaHook?.avatarMood || 'SPEAKING');

      if (activeSlide.type === 'STEPS') {
        const stepList = this.steps() || [];
        const currentCount = untracked(() => this.revealedStepCount());
        if (currentCount === 0 && stepList.length > 0) {
          this.revealedStepCount.set(1);
          this.activeCaption.set(stepList[0]?.explanation || activeSlide.narrationText);
        } else if (currentCount > 0 && stepList.length >= currentCount) {
          this.activeCaption.set(stepList[currentCount - 1]?.explanation || activeSlide.narrationText);
        } else {
          this.activeCaption.set(activeSlide.narrationText);
        }
      } else {
        this.activeCaption.set(activeSlide.narrationText);
      }

      const stepMs = Math.max(40, Math.round(3200 / speed / 20));
      let ticks = 0;

      this.autoPlayTimer = setInterval(() => {
        ticks += 1;
        this.autoPlayProgress.set(Math.min(100, ticks * 5));
        if (ticks >= 20) {
          ticks = 0;
          this.autoPlayProgress.set(0);
          const stepList = this.steps() || [];
          const totalSteps = stepList.length;
          const currentCount = this.revealedStepCount();
          if (activeSlide.type === 'STEPS' && currentCount < totalSteps) {
            const nextCount = currentCount + 1;
            this.revealedStepCount.set(nextCount);
            const currentStep = stepList[nextCount - 1];
            if (currentStep?.explanation) {
              this.activeCaption.set(currentStep.explanation);
            }
          } else if (slideIdx < slideList.length - 1) {
            this.currentSlideIndex.set(slideIdx + 1);
          } else {
            this.isPlaying.set(false);
          }
        }
      }, stepMs);
    });
  }

  ngOnDestroy(): void {
    this.clearAutoPlayTimer();
  }

  onSelectSlide(index: number): void {
    if (index >= 0 && index < this.totalSlides()) {
      this.currentSlideIndex.set(index);
    }
  }

  onNextSlide(): void {
    if (this.currentSlideIndex() < this.totalSlides() - 1) {
      this.currentSlideIndex.update((i) => i + 1);
    }
  }

  onPrevSlide(): void {
    if (this.currentSlideIndex() > 0) {
      this.currentSlideIndex.update((i) => i - 1);
    }
  }

  onTogglePlay(): void {
    const nextState = !this.isPlaying();
    if (nextState && this.currentSlideIndex() >= this.totalSlides() - 1) {
      this.currentSlideIndex.set(0);
    }
    this.isPlaying.set(nextState);
    this.playbackMode.set(nextState ? 'AUTO' : 'MANUAL');
  }

  onStartAutoPlay(): void {
    if (this.currentSlideIndex() === 0) this.currentSlideIndex.set(1);
    this.isPlaying.set(true);
    this.playbackMode.set('AUTO');
  }

  onSpeedChange(speed: number): void {
    this.playbackSpeed.set(speed);
  }

  onToggleAudio(): void {
    this.audioEnabled.update((v) => !v);
  }

  onToggleTutorStage(): void {
    this.tutorStageExpanded.update((v) => !v);
  }

  onRevealNextStep(): void {
    const max = this.steps()?.length || 0;
    if (this.revealedStepCount() < max) {
      this.revealedStepCount.update((c) => c + 1);
    }
  }

  onRevealAllSteps(): void {
    this.revealedStepCount.set(this.steps()?.length || 0);
  }

  onResetSteps(): void {
    this.revealedStepCount.set(0);
  }

  onSelectQuizOption(event: { questionIndex: number; option: string }): void {
    const items = this.previewLesson().content.quickCheckItems || [];
    const q = items[event.questionIndex];
    const correctAns = q ? (q.correctAnswer || q.answer) : event.option;
    const isCorrect = event.option === correctAns;
    this.quizSelections.update((prev) => ({
      ...prev,
      [event.questionIndex]: { selectedOption: event.option, isCorrect },
    }));
  }

  onResetQuiz(): void {
    this.quizSelections.set({});
  }

  formatLatex(latex: string): string {
    const trimmed = (latex || '').trim();
    if (!trimmed) return '';
    if (trimmed.includes('$$') || trimmed.includes('$')) return trimmed;
    return `$$${trimmed}$$`;
  }

  private clearAutoPlayTimer(): void {
    if (this.autoPlayTimer) {
      clearInterval(this.autoPlayTimer);
      this.autoPlayTimer = null;
    }
  }
}
