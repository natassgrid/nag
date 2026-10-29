import {
  Component,
  ChangeDetectionStrategy,
  OnInit,
  OnDestroy,
  ElementRef,
  HostListener,
  ViewChild,
  inject,
  signal,
  computed,
  effect,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import {
  SubtopicMicroLesson,
  LessonSlide,
  MicroTutorialApproach,
  QuizSelectionState,
  LessonPlaybackMode,
  TutorAvatarState,
  createSlidesFromLesson,
  extractStepsFromLesson,
} from '../../models/micro-lesson.model';
import { LessonType, LessonDifficultyLevel } from '../../models/learner.model';
import { MicroLessonService, MicroLessonQueryParams } from '../../services/micro-lesson.service';
import { AdaptiveLearningService } from '../../services/adaptive-learning.service';
import { LearningPathService } from '../../services/learning-path.service';
import { MicroLessonPlayerTopbarComponent } from '../micro-lesson-player-topbar/micro-lesson-player-topbar.component';
import { MicroLessonVariantSwitcherComponent } from '../micro-lesson-variant-switcher/micro-lesson-variant-switcher.component';
import { MicroLessonSlideContainerComponent } from '../micro-lesson-slide-container/micro-lesson-slide-container.component';
import { MicroLessonFeedbackBarComponent } from '../micro-lesson-feedback-bar/micro-lesson-feedback-bar.component';

@Component({
  selector: 'app-micro-lesson-viewer',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MicroLessonPlayerTopbarComponent,
    MicroLessonVariantSwitcherComponent,
    MicroLessonSlideContainerComponent,
    MicroLessonFeedbackBarComponent,
  ],
  templateUrl: './micro-lesson-viewer.component.html',
  styleUrl: './micro-lesson-viewer.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MicroLessonViewerComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly microLessonService = inject(MicroLessonService);
  private readonly learningService = inject(AdaptiveLearningService);
  private readonly learningPathService = inject(LearningPathService);

  subtopicIdInput = input<number | null>(null, { alias: 'subtopicId' });
  milestoneIdInput = input<string | null>(null, { alias: 'milestoneId' });
  completed = output<string>();

  readonly lesson = signal<SubtopicMicroLesson | null>(null);
  readonly allVariants = signal<SubtopicMicroLesson[]>([]);
  readonly approaches = signal<MicroTutorialApproach[]>([]);
  readonly activeApproachId = signal<string>('');

  readonly loading = signal<boolean>(true);
  readonly completing = signal<boolean>(false);
  readonly isCompleted = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly currentType = signal<LessonType>('CONCEPT');
  readonly currentDifficulty = signal<LessonDifficultyLevel>('CORE');
  readonly userHelpfulRating = signal<number | null>(null);
  readonly selectedLanguage = signal<string>('en');

  // Interactive Slide Player & AI Tutor Signals
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
  readonly isFullscreen = signal<boolean>(false);

  @ViewChild('playerContainer') playerContainerRef?: ElementRef<HTMLElement>;

  private autoPlayTimer: ReturnType<typeof setInterval> | null = null;

  readonly slides = computed<LessonSlide[]>(() => {
    const l = this.lesson();
    if (!l) return [];
    if (typeof this.microLessonService.buildSlidesForLesson === 'function') {
      return this.microLessonService.buildSlidesForLesson(l);
    }
    return createSlidesFromLesson(l);
  });

  readonly totalSlides = computed<number>(() => this.slides().length || 5);

  resolvedSubtopicId: number | null = null;
  resolvedMilestoneId: string | null = null;
  resolvedQueryParams: MicroLessonQueryParams = {};

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

      const steps = extractStepsFromLesson(this.lesson());
      const totalSteps = steps.length;

      this.tutorAvatarState.set(activeSlide.mediaHook?.avatarMood || 'SPEAKING');
      if (activeSlide.type === 'STEPS' && totalSteps > 0) {
        if (this.revealedStepCount() === 0) {
          this.revealedStepCount.set(1);
        }
        const currentStep = steps[Math.min(this.revealedStepCount(), totalSteps) - 1];
        if (currentStep) {
          this.speakNarration(`${currentStep.title}: ${currentStep.explanation}`);
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
          const latestSteps = extractStepsFromLesson(this.lesson());
          const maxSteps = latestSteps.length;
          if (activeSlide.type === 'STEPS' && this.revealedStepCount() < maxSteps) {
            const nextCount = this.revealedStepCount() + 1;
            this.revealedStepCount.set(nextCount);
            const nextStep = latestSteps[nextCount - 1];
            if (nextStep) {
              this.speakNarration(`${nextStep.title}: ${nextStep.explanation}`);
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

  ngOnInit(): void {
    const routeSubId = this.route.snapshot.paramMap.get('subtopicId');
    const qp = this.route.snapshot.queryParamMap;
    this.resolvedSubtopicId = this.subtopicIdInput() || (routeSubId ? Number(routeSubId) : 301);
    this.resolvedMilestoneId = this.milestoneIdInput() || qp.get('milestoneId') || null;
    const routeLang = qp.get('lang');
    if (routeLang) this.selectedLanguage.set(routeLang);
    this.resolvedQueryParams = {
      subtopicName: qp.get('subtopicName') || undefined,
      topicName: qp.get('topicName') || undefined,
      targetExam: qp.get('targetExam') || undefined,
      lang: routeLang || 'en',
    };

    this.currentType.set((qp.get('lessonType') as LessonType) || 'CONCEPT');
    this.currentDifficulty.set((qp.get('difficulty') as LessonDifficultyLevel) || 'CORE');

    if (this.resolvedSubtopicId) {
      this.loadLesson(this.resolvedSubtopicId);
    } else {
      this.error.set('No subtopic identifier specified for micro-lesson.');
      this.loading.set(false);
    }
  }

  ngOnDestroy(): void {
    this.clearAutoPlayTimer();
    this.stopSpeech();
  }

  loadLesson(subtopicId: number): void {
    this.loading.set(true);
    this.error.set(null);
    const params: MicroLessonQueryParams = {
      ...this.resolvedQueryParams,
      lessonType: this.currentType(),
      difficulty: this.currentDifficulty(),
      lang: this.selectedLanguage(),
    };

    if (typeof this.microLessonService.getMicroLessonsBySubtopic === 'function') {
      this.microLessonService.getMicroLessonsBySubtopic(subtopicId, params).subscribe({
        next: (lessons) => {
          if (lessons && lessons.length > 0) {
            this.allVariants.set(lessons);
            const matched =
              lessons.find((l) => l.lessonType === this.currentType() && l.difficultyLevel === this.currentDifficulty()) ||
              lessons[0];
            this.applyActiveLesson(matched, lessons);
            this.loading.set(false);
          } else {
            this.fallbackPublishedLesson(subtopicId, params);
          }
        },
        error: () => this.fallbackPublishedLesson(subtopicId, params),
      });
    } else {
      this.fallbackPublishedLesson(subtopicId, params);
    }
  }

  private fallbackPublishedLesson(subtopicId: number, params: MicroLessonQueryParams): void {
    this.microLessonService.getPublishedMicroLesson(subtopicId, params).subscribe({
      next: (data) => {
        if (data) {
          this.allVariants.set([data]);
          this.applyActiveLesson(data, [data]);
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Micro-lesson content is being generated for this subtopic.');
        this.loading.set(false);
      },
    });
  }

  private applyActiveLesson(selected: SubtopicMicroLesson, variants: SubtopicMicroLesson[]): void {
    this.lesson.set(selected);
    if (selected.languageCode) this.selectedLanguage.set(selected.languageCode);
    if (typeof this.microLessonService.buildApproachesForSubtopic === 'function') {
      const built = this.microLessonService.buildApproachesForSubtopic(selected, variants);
      this.approaches.set(built);
      const active =
        built.find((a) => a.lesson?.id && a.lesson.id === selected.id) ||
        built.find((a) => a.id === selected.id) ||
        built.find((a) => a.sequenceNo === selected.sequenceNo) ||
        built[0];
      if (active) this.activeApproachId.set(active.id);
    }
    this.resetSlideInteractiveState();
  }

  onApproachChange(approach: MicroTutorialApproach): void {
    this.activeApproachId.set(approach.id);
    this.currentType.set(approach.lessonType as LessonType);
    this.currentDifficulty.set(approach.difficultyLevel as LessonDifficultyLevel);
    this.lesson.set(approach.lesson);
    this.resetSlideInteractiveState();
  }

  changeLanguage(lang: string): void {
    if (this.selectedLanguage() === lang) return;
    this.selectedLanguage.set(lang);
    const current = this.lesson();
    if (current?.id && typeof this.microLessonService.getMicroLessonByLanguage === 'function') {
      this.microLessonService.getMicroLessonByLanguage(current.id, lang).subscribe({
        next: (data) => this.applyActiveLesson(data, this.allVariants()),
        error: () => this.resolvedSubtopicId && this.loadLesson(this.resolvedSubtopicId),
      });
    } else if (this.resolvedSubtopicId) {
      this.loadLesson(this.resolvedSubtopicId);
    }
  }

  onTypeChange(type: LessonType): void {
    this.currentType.set(type);
    const existing = this.approaches().find((a) => a.lessonType === type);
    if (existing) {
      this.onApproachChange(existing);
      return;
    }
    if (this.resolvedSubtopicId) this.loadLesson(this.resolvedSubtopicId);
  }

  onDifficultyChange(level: LessonDifficultyLevel): void {
    this.currentDifficulty.set(level);
    if (this.resolvedSubtopicId) this.loadLesson(this.resolvedSubtopicId);
  }

  onTryAnotherExplanation(): void {
    const list = this.approaches();
    if (list.length > 1) {
      const idx = list.findIndex((a) => a.id === this.activeApproachId());
      const next = list[(idx + 1) % list.length];
      this.onApproachChange(next);
    }
  }

  onSelectSlide(index: number): void {
    if (index >= 0 && index < this.totalSlides()) {
      this.currentSlideIndex.set(index);
      const slide = this.slides()[index];
      if (slide) this.speakNarration(slide.narrationText);
    }
  }

  onNextSlide(): void {
    if (this.currentSlideIndex() < this.totalSlides() - 1) {
      this.onSelectSlide(this.currentSlideIndex() + 1);
    }
  }

  onPrevSlide(): void {
    if (this.currentSlideIndex() > 0) {
      this.onSelectSlide(this.currentSlideIndex() - 1);
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
    const enabled = !this.audioEnabled();
    this.audioEnabled.set(enabled);
    if (enabled) {
      const slide = this.slides()[this.currentSlideIndex()];
      if (slide) this.speakNarration(slide.narrationText);
    } else {
      this.stopSpeech();
    }
  }

  onToggleTutorStage(): void {
    this.tutorStageExpanded.update((v) => !v);
  }

  onRevealNextStep(): void {
    const steps = extractStepsFromLesson(this.lesson());
    const max = steps.length;
    if (this.revealedStepCount() < max) {
      const nextCount = this.revealedStepCount() + 1;
      this.revealedStepCount.set(nextCount);
      const revealedStep = steps[nextCount - 1];
      if (revealedStep) {
        this.speakNarration(`${revealedStep.title}: ${revealedStep.explanation}`);
      }
    }
  }

  onRevealAllSteps(): void {
    const max = extractStepsFromLesson(this.lesson()).length;
    this.revealedStepCount.set(max);
  }

  onResetSteps(): void {
    this.revealedStepCount.set(0);
  }

  onSelectQuizOption(event: { questionIndex: number; option: string }): void {
    const items = this.lesson()?.content?.quickCheckItems || [];
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

  onRateHelpful(rating: number): void {
    this.userHelpfulRating.set(rating);
  }

  onMarkCompleted(): void {
    this.completing.set(true);
    const enrollment = this.learningService.activeEnrollment();
    const aid = this.resolvedMilestoneId || `act-sub-${this.resolvedSubtopicId || 301}`;

    if (enrollment) {
      this.learningService.completeActivity(enrollment.id, aid, { status: 'COMPLETED', helpfulRating: this.userHelpfulRating() || 1 }).subscribe();
    }

    if (this.resolvedMilestoneId) {
      this.learningPathService.completeMilestone(this.resolvedMilestoneId).subscribe({
        next: () => this.finishCompletion(this.resolvedMilestoneId!),
        error: () => this.finishCompletion(this.resolvedMilestoneId!),
      });
    } else {
      this.finishCompletion('');
    }
  }

  private finishCompletion(id: string): void {
    this.completing.set(false);
    this.isCompleted.set(true);
    this.completed.emit(id);
  }

  onBackToRoadmap(): void {
    this.router.navigate(['/learn/roadmap']);
  }

  @HostListener('document:fullscreenchange')
  @HostListener('document:webkitfullscreenchange')
  onFullscreenChange(): void {
    if (typeof document !== 'undefined') {
      const isDocFull = !!(
        document.fullscreenElement ||
        (document as any).webkitFullscreenElement
      );
      this.isFullscreen.set(isDocFull);
    }
  }

  onToggleFullscreen(): void {
    if (typeof document === 'undefined') return;
    const target = this.playerContainerRef?.nativeElement;

    if (!this.isFullscreen()) {
      if (target?.requestFullscreen) {
        target
          .requestFullscreen()
          .then(() => this.isFullscreen.set(true))
          .catch(() => this.isFullscreen.set(true));
      } else if ((target as any)?.webkitRequestFullscreen) {
        (target as any).webkitRequestFullscreen();
        this.isFullscreen.set(true);
      } else {
        this.isFullscreen.set(true);
      }
    } else {
      if (
        document.fullscreenElement ||
        (document as any).webkitFullscreenElement
      ) {
        if (document.exitFullscreen) {
          document
            .exitFullscreen()
            .then(() => this.isFullscreen.set(false))
            .catch(() => this.isFullscreen.set(false));
        } else if ((document as any).webkitExitFullscreen) {
          (document as any).webkitExitFullscreen();
          this.isFullscreen.set(false);
        }
      } else {
        this.isFullscreen.set(false);
      }
    }
  }

  private resetSlideInteractiveState(): void {
    this.currentSlideIndex.set(0);
    this.revealedStepCount.set(0);
    this.quizSelections.set({});
  }

  private speakNarration(text: string): void {
    this.activeCaption.set(text);
    if (!this.audioEnabled() || typeof window === 'undefined' || !('speechSynthesis' in window)) return;
    try {
      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.rate = this.playbackSpeed();
      window.speechSynthesis.speak(utterance);
    } catch {
      // Safe fallback when Web Speech API is unavailable in headless environments
    }
  }

  private stopSpeech(): void {
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      try {
        window.speechSynthesis.cancel();
      } catch {
        // Ignore in non-browser environment
      }
    }
  }

  private clearAutoPlayTimer(): void {
    if (this.autoPlayTimer) {
      clearInterval(this.autoPlayTimer);
      this.autoPlayTimer = null;
    }
  }
}
