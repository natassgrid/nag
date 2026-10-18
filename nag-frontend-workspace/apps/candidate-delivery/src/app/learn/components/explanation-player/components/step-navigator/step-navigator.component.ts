import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ExplanationStep } from '../../../../models/explanation-ir.model';

@Component({
  selector: 'nag-step-navigator',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex flex-col gap-3 p-4 bg-slate-50 dark:bg-slate-800/80 rounded-xl border border-slate-200 dark:border-slate-700/60 shadow-xs">
      <!-- Top controls: Play/Pause, Stepper arrows, and Playback Speed -->
      <div class="flex items-center justify-between flex-wrap gap-2">
        <div class="flex items-center gap-2">
          <button
            mat-icon-button
            type="button"
            (click)="onTogglePlay()"
            [attr.aria-label]="isPlaying() ? 'Pause auto-play' : 'Start auto-play'"
            class="text-indigo-600 dark:text-indigo-400"
          >
            <mat-icon>{{ isPlaying() ? 'pause' : 'play_arrow' }}</mat-icon>
          </button>

          <span class="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
            Step {{ activeStepIndex() + 1 }} of {{ steps().length }}
          </span>
        </div>

        <!-- Navigation arrows -->
        <div class="flex items-center gap-1">
          <button
            mat-stroked-button
            type="button"
            [disabled]="activeStepIndex() <= 0"
            (click)="onPrevious()"
            class="text-xs"
          >
            <mat-icon>chevron_left</mat-icon>
            Prev
          </button>
          <button
            mat-flat-button
            type="button"
            color="primary"
            [disabled]="activeStepIndex() >= steps().length - 1"
            (click)="onNext()"
            class="text-xs"
          >
            Next
            <mat-icon>chevron_right</mat-icon>
          </button>
        </div>

        <!-- Playback speed pills -->
        <div class="flex items-center gap-1 bg-white dark:bg-slate-900 px-2 py-1 rounded-lg border border-slate-200 dark:border-slate-700">
          <span class="text-[10px] uppercase font-bold text-slate-400 mr-1">Speed</span>
          @for (speed of speeds; track speed) {
            <button
              type="button"
              (click)="onSelectSpeed(speed)"
              [class.bg-indigo-600]="playbackSpeed() === speed"
              [class.text-white]="playbackSpeed() === speed"
              [class.text-slate-600]="playbackSpeed() !== speed"
              [class.dark:text-slate-300]="playbackSpeed() !== speed"
              class="px-2 py-0.5 text-xs font-medium rounded-md transition-colors hover:bg-indigo-50 dark:hover:bg-slate-800"
            >
              {{ speed }}x
            </button>
          }
        </div>
      </div>

      <!-- Step sequence track pills -->
      <div class="flex items-center gap-2 overflow-x-auto pb-1 pt-1 scrollbar-thin">
        @for (step of steps(); track step.id; let idx = $index) {
          <button
            type="button"
            (click)="onSelectStep(idx)"
            [class.bg-indigo-600]="activeStepIndex() === idx"
            [class.text-white]="activeStepIndex() === idx"
            [class.shadow-md]="activeStepIndex() === idx"
            [class.bg-white]="activeStepIndex() !== idx && activeStepIndex() < idx"
            [class.dark:bg-slate-900]="activeStepIndex() !== idx && activeStepIndex() < idx"
            [class.bg-emerald-50]="activeStepIndex() > idx"
            [class.dark:bg-emerald-950/40]="activeStepIndex() > idx"
            [class.border-emerald-400]="activeStepIndex() > idx"
            [class.text-emerald-700]="activeStepIndex() > idx"
            [class.dark:text-emerald-300]="activeStepIndex() > idx"
            class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium border border-slate-200 dark:border-slate-700 whitespace-nowrap transition-all hover:scale-102"
          >
            @if (activeStepIndex() > idx) {
              <mat-icon class="!w-3.5 !h-3.5 !text-sm text-emerald-500">check_circle</mat-icon>
            } @else {
              <span class="w-4 h-4 rounded-full flex items-center justify-center text-[10px] bg-slate-200 dark:bg-slate-800">
                {{ idx + 1 }}
              </span>
            }
            <span>{{ step.type }}</span>
          </button>
        }
      </div>
    </div>
  `,
  styles: [
    `
      :host {
        display: block;
      }
    `,
  ],
})
export class StepNavigatorComponent {
  steps = input.required<ExplanationStep[]>();
  activeStepIndex = input.required<number>();
  isPlaying = input<boolean>(false);
  playbackSpeed = input<number>(1.0);

  stepChange = output<number>();
  playToggle = output<boolean>();
  speedChange = output<number>();

  readonly speeds = [0.5, 1.0, 1.5, 2.0];

  onPrevious(): void {
    if (this.activeStepIndex() > 0) {
      this.stepChange.emit(this.activeStepIndex() - 1);
    }
  }

  onNext(): void {
    if (this.activeStepIndex() < this.steps().length - 1) {
      this.stepChange.emit(this.activeStepIndex() + 1);
    }
  }

  onSelectStep(index: number): void {
    this.stepChange.emit(index);
  }

  onTogglePlay(): void {
    this.playToggle.emit(!this.isPlaying());
  }

  onSelectSpeed(speed: number): void {
    this.speedChange.emit(speed);
  }
}
