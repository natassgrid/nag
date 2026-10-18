import {
  Component,
  ChangeDetectionStrategy,
  input,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { ExplanationStep } from '../../../../models/explanation-ir.model';
import { MathRendererComponent } from '../math-renderer/math-renderer.component';

@Component({
  selector: 'nag-step-renderer',
  standalone: true,
  imports: [CommonModule, MatChipsModule, MatIconModule, MathRendererComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200/90 dark:border-slate-800 shadow-sm flex flex-col gap-4">
      <!-- Step header -->
      <div class="flex items-center justify-between flex-wrap gap-2">
        <div class="flex items-center gap-2.5">
          <span class="w-7 h-7 rounded-full bg-indigo-600 text-white font-bold text-xs flex items-center justify-center shadow-xs">
            {{ step().sequence }}
          </span>
          <span
            [class.bg-blue-100]="step().type === 'GIVEN'"
            [class.text-blue-800]="step().type === 'GIVEN'"
            [class.dark:bg-blue-900/40]="step().type === 'GIVEN'"
            [class.dark:text-blue-300]="step().type === 'GIVEN'"
            [class.bg-purple-100]="step().type === 'APPLY_RULE'"
            [class.text-purple-800]="step().type === 'APPLY_RULE'"
            [class.dark:bg-purple-900/40]="step().type === 'APPLY_RULE'"
            [class.dark:text-purple-300]="step().type === 'APPLY_RULE'"
            [class.bg-amber-100]="step().type === 'TRANSFORM'"
            [class.text-amber-800]="step().type === 'TRANSFORM'"
            [class.dark:bg-amber-900/40]="step().type === 'TRANSFORM'"
            [class.dark:text-amber-300]="step().type === 'TRANSFORM'"
            [class.bg-emerald-100]="step().type === 'CONCLUDE'"
            [class.text-emerald-800]="step().type === 'CONCLUDE'"
            [class.dark:bg-emerald-900/40]="step().type === 'CONCLUDE'"
            [class.dark:text-emerald-300]="step().type === 'CONCLUDE'"
            class="px-2.5 py-1 rounded-md text-xs font-bold uppercase tracking-wide border border-transparent"
          >
            {{ step().type }}
          </span>
        </div>

        <!-- Representations pills -->
        @if (step().representations && step().representations!.length > 0) {
          <div class="flex items-center gap-1">
            @for (rep of step().representations!; track rep) {
              <span class="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400">
                {{ rep }}
              </span>
            }
          </div>
        }
      </div>

      <!-- Operation pill -->
      @if (step().operation) {
        <div class="inline-flex items-center gap-2 px-3 py-1.5 rounded-lg bg-indigo-50/70 dark:bg-indigo-950/40 border border-indigo-200/60 dark:border-indigo-800/40 text-xs text-indigo-900 dark:text-indigo-300 font-medium">
          <mat-icon class="!w-4 !h-4 !text-base text-indigo-600 dark:text-indigo-400">bolt</mat-icon>
          <span class="font-bold">Operation:</span>
          <span class="font-mono">{{ step().operation?.name }}</span>
          @if (step().operation?.target) {
            <span class="text-indigo-600 dark:text-indigo-400 font-mono">({{ step().operation?.target }})</span>
          }
          @if (step().operation?.value !== undefined) {
            <span class="text-indigo-700 dark:text-indigo-300 font-mono">val: {{ step().operation?.value }}</span>
          }
        </div>
      }

      <!-- Before and After transition cards -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
        @if (step().before) {
          <div class="p-3.5 rounded-xl bg-slate-50/80 dark:bg-slate-800/50 border border-slate-200/80 dark:border-slate-700/60 flex flex-col gap-1.5">
            <span class="text-[11px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
              State Before:
            </span>
            <div class="text-sm font-serif text-slate-800 dark:text-slate-200">
              <nag-explanation-math-renderer [content]="step().before!"></nag-explanation-math-renderer>
            </div>
          </div>
        }

        @if (step().after) {
          <div
            [class.md:col-span-2]="!step().before"
            class="p-3.5 rounded-xl bg-emerald-50/50 dark:bg-emerald-950/20 border border-emerald-200/80 dark:border-emerald-800/40 flex flex-col gap-1.5"
          >
            <span class="text-[11px] font-bold uppercase tracking-wider text-emerald-700 dark:text-emerald-400 flex items-center gap-1">
              <mat-icon class="!w-3.5 !h-3.5 !text-sm">arrow_forward</mat-icon>
              Resulting Expression:
            </span>
            <div class="text-sm font-serif text-emerald-950 dark:text-emerald-100 font-medium">
              <nag-explanation-math-renderer [content]="step().after!"></nag-explanation-math-renderer>
            </div>
          </div>
        }
      </div>

      <!-- Mathematical formula details -->
      @if (step().math?.latex) {
        <div class="p-3 rounded-lg bg-slate-50 dark:bg-slate-800/60 border border-slate-200/60 dark:border-slate-700/40 text-center">
          <nag-explanation-math-renderer [content]="'$$' + step().math!.latex + '$$'"></nag-explanation-math-renderer>
        </div>
      }
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
export class StepRendererComponent {
  step = input.required<ExplanationStep>();
}
