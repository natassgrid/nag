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
import {
  PlayerSubtopicMicroLesson,
  LessonSlide,
  QuizSelectionState,
  TutorAvatarState,
} from '../../micro-lesson-player.model';
import {
  IntroSlideComponent,
  ConceptSlideComponent,
  FormulaSlideComponent,
  StepsSlideComponent,
  QuizSlideComponent,
} from '../slides';

@Component({
  selector: 'nag-micro-lesson-slide-container, app-micro-lesson-slide-container',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    IntroSlideComponent,
    ConceptSlideComponent,
    FormulaSlideComponent,
    StepsSlideComponent,
    QuizSlideComponent,
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
