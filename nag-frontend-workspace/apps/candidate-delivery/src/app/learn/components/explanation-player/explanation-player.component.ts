import {
  Component,
  ChangeDetectionStrategy,
  OnDestroy,
  computed,
  effect,
  input,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';
import { ExplanationIr, ExplanationStep } from '../../models/explanation-ir.model';
import { MathRendererComponent } from './components/math-renderer/math-renderer.component';
import { StepNavigatorComponent } from './components/step-navigator/step-navigator.component';
import { StepRendererComponent } from './components/step-renderer/step-renderer.component';
import { WhyPanelComponent } from './components/why-panel/why-panel.component';
import { HintPanelComponent } from './components/hint-panel/hint-panel.component';
import { VerificationPanelComponent } from './components/verification-panel/verification-panel.component';
import { MisconceptionPanelComponent } from './components/misconception-panel/misconception-panel.component';

@Component({
  selector: 'nag-explanation-player',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatChipsModule,
    MathRendererComponent,
    StepNavigatorComponent,
    StepRendererComponent,
    WhyPanelComponent,
    HintPanelComponent,
    VerificationPanelComponent,
    MisconceptionPanelComponent,
  ],
  templateUrl: './explanation-player.component.html',
  styleUrl: './explanation-player.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExplanationPlayerComponent implements OnDestroy {
  readonly explanation = input.required<ExplanationIr>();

  readonly activeStepIndex = signal<number>(0);
  readonly isPlaying = signal<boolean>(false);
  readonly playbackSpeed = signal<number>(1.0);

  private playbackTimer: ReturnType<typeof setInterval> | null = null;

  readonly steps = computed<ExplanationStep[]>(() => {
    return this.explanation()?.steps ?? [];
  });

  readonly currentStep = computed<ExplanationStep | undefined>(() => {
    const s = this.steps();
    return s[this.activeStepIndex()] ?? s[0];
  });

  readonly progressPercent = computed<number>(() => {
    const total = this.steps().length;
    if (total <= 1) return 100;
    return Math.round(((this.activeStepIndex() + 1) / total) * 100);
  });

  constructor() {
    // React to play/speed changes
    effect(() => {
      const playing = this.isPlaying();
      const speed = this.playbackSpeed();
      this.clearTimer();

      if (playing) {
        const intervalMs = Math.max(500, Math.round(3000 / speed));
        this.playbackTimer = setInterval(() => {
          const current = this.activeStepIndex();
          const total = this.steps().length;
          if (current < total - 1) {
            this.activeStepIndex.set(current + 1);
          } else {
            this.isPlaying.set(false);
          }
        }, intervalMs);
      }
    });
  }

  ngOnDestroy(): void {
    this.clearTimer();
  }

  onSelectStep(index: number): void {
    const total = this.steps().length;
    if (index >= 0 && index < total) {
      this.activeStepIndex.set(index);
    }
  }

  onTogglePlay(play: boolean): void {
    if (play && this.activeStepIndex() >= this.steps().length - 1) {
      this.activeStepIndex.set(0);
    }
    this.isPlaying.set(play);
  }

  onSelectSpeed(speed: number): void {
    this.playbackSpeed.set(speed);
  }

  private clearTimer(): void {
    if (this.playbackTimer) {
      clearInterval(this.playbackTimer);
      this.playbackTimer = null;
    }
  }
}
