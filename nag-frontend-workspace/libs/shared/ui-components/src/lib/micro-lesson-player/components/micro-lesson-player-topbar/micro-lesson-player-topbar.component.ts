import {
  Component,
  ChangeDetectionStrategy,
  computed,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import {
  LessonSlide,
  LessonPlaybackMode,
  TutorAvatarState,
} from '../../micro-lesson-player.model';

@Component({
  selector: 'nag-micro-lesson-player-topbar, app-micro-lesson-player-topbar',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
  ],
  templateUrl: './micro-lesson-player-topbar.component.html',
  styleUrl: './micro-lesson-player-topbar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MicroLessonPlayerTopbarComponent {
  readonly subjectName = input<string>('Curriculum Subject');
  readonly topicName = input<string>('Topic');
  readonly subtopicName = input<string>('Micro-Lesson');
  readonly approachLabel = input<string>('Conceptual Foundation');
  readonly contentVersion = input<number>(1);
  readonly showBackButton = input<boolean>(true);

  readonly slides = input<LessonSlide[]>([]);
  readonly currentSlideIndex = input<number>(0);
  readonly totalSlides = input<number>(5);

  readonly isPlaying = input<boolean>(false);
  readonly playbackMode = input<LessonPlaybackMode>('MANUAL');
  readonly playbackSpeed = input<number>(1);
  readonly autoPlayProgress = input<number>(0);
  readonly audioEnabled = input<boolean>(false);
  readonly tutorStageExpanded = input<boolean>(false);
  readonly tutorAvatarState = input<TutorAvatarState>('IDLE');
  readonly isFullscreen = input<boolean>(false);

  readonly backToRoadmap = output<void>();
  readonly selectSlide = output<number>();
  readonly togglePlay = output<void>();
  readonly speedChange = output<number>();
  readonly toggleAudio = output<void>();
  readonly toggleTutorStage = output<void>();
  readonly toggleFullscreen = output<void>();

  readonly speeds: number[] = [0.75, 1, 1.25, 1.5, 2];

  readonly currentSlideNumber = computed<number>(() => {
    const total = this.totalSlides() || 1;
    return Math.min(total, Math.max(1, this.currentSlideIndex() + 1));
  });

  readonly overallProgressPercent = computed<number>(() => {
    const total = this.totalSlides() || 1;
    return Math.round((this.currentSlideNumber() / total) * 100);
  });

  onBack(): void {
    this.backToRoadmap.emit();
  }

  onSelectSlide(index: number): void {
    this.selectSlide.emit(index);
  }

  onTogglePlay(): void {
    this.togglePlay.emit();
  }

  onCycleSpeed(): void {
    const current = this.playbackSpeed();
    const idx = this.speeds.indexOf(current);
    const next = this.speeds[(idx + 1) % this.speeds.length];
    this.speedChange.emit(next);
  }

  onToggleAudio(): void {
    this.toggleAudio.emit();
  }

  onToggleTutorStage(): void {
    this.toggleTutorStage.emit();
  }

  onToggleFullscreen(): void {
    this.toggleFullscreen.emit();
  }
}
