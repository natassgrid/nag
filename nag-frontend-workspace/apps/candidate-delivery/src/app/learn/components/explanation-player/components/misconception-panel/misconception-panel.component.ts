import {
  Component,
  ChangeDetectionStrategy,
  input,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { ExplanationMisconception } from '../../../../models/explanation-ir.model';
import { MathRendererComponent } from '../math-renderer/math-renderer.component';

@Component({
  selector: 'nag-misconception-panel',
  standalone: true,
  imports: [CommonModule, MatIconModule, MathRendererComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (activeItem()) {
      <div class="p-4 rounded-xl bg-rose-50/70 dark:bg-rose-950/30 border border-rose-200 dark:border-rose-800/50 shadow-xs flex flex-col gap-2.5">
        <div class="flex items-center gap-2 text-rose-800 dark:text-rose-300 font-semibold text-sm">
          <mat-icon class="!w-4 !h-4 !text-base">error_outline</mat-icon>
          <span>Common Student Trap / Misconception</span>
        </div>

        <div class="flex flex-col gap-2 text-xs">
          @if (activeItem()?.incorrectReasoning) {
            <div class="p-2 rounded-lg bg-white/80 dark:bg-slate-900/80 border border-rose-200/60 dark:border-rose-800/40">
              <span class="font-bold text-rose-700 dark:text-rose-400 block mb-0.5">Common Pitfall:</span>
              <nag-explanation-math-renderer [content]="activeItem()!.incorrectReasoning"></nag-explanation-math-renderer>
            </div>
          }

          @if (activeItem()?.whyWrong) {
            <div class="text-rose-900 dark:text-rose-200 leading-relaxed px-1">
              <span class="font-bold">Why it fails: </span>
              <nag-explanation-math-renderer [content]="activeItem()!.whyWrong" [inline]="true"></nag-explanation-math-renderer>
            </div>
          }

          @if (activeItem()?.correctedReasoning) {
            <div class="p-2 rounded-lg bg-emerald-50/90 dark:bg-emerald-950/60 border border-emerald-200/60 dark:border-emerald-800/40 text-emerald-900 dark:text-emerald-200">
              <span class="font-bold text-emerald-800 dark:text-emerald-300 block mb-0.5 flex items-center gap-1">
                <mat-icon class="!w-3.5 !h-3.5 !text-sm text-emerald-600">check_circle</mat-icon>
                Correct Strategy:
              </span>
              <nag-explanation-math-renderer [content]="activeItem()!.correctedReasoning"></nag-explanation-math-renderer>
            </div>
          }
        </div>
      </div>
    }
  `,
  styles: [
    `
      :host {
        display: block;
      }
    `,
  ],
})
export class MisconceptionPanelComponent {
  misconception = input<ExplanationMisconception | undefined>();
  misconceptionsList = input<ExplanationMisconception[] | undefined>();

  activeItem(): ExplanationMisconception | undefined {
    return this.misconception() || (this.misconceptionsList() && this.misconceptionsList()![0]);
  }
}
