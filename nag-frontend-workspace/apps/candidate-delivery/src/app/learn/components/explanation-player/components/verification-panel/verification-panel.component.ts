import {
  Component,
  ChangeDetectionStrategy,
  input,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { VerificationResult } from '../../../../models/explanation-ir.model';

@Component({
  selector: 'nag-verification-panel',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (verification()) {
      <div
        [class.bg-emerald-50/70]="verification()!.passed"
        [class.dark:bg-emerald-950/30]="verification()!.passed"
        [class.border-emerald-200]="verification()!.passed"
        [class.dark:border-emerald-800/50]="verification()!.passed"
        [class.bg-rose-50/70]="!verification()!.passed"
        [class.dark:bg-rose-950/30]="!verification()!.passed"
        [class.border-rose-200]="!verification()!.passed"
        [class.dark:border-rose-800/50]="!verification()!.passed"
        class="p-4 rounded-xl border shadow-xs flex flex-col gap-2"
      >
        <div class="flex items-center justify-between">
          <div
            [class.text-emerald-800]="verification()!.passed"
            [class.dark:text-emerald-300]="verification()!.passed"
            [class.text-rose-800]="!verification()!.passed"
            [class.dark:text-rose-300]="!verification()!.passed"
            class="flex items-center gap-2 font-semibold text-sm"
          >
            <mat-icon class="!w-4 !h-4 !text-base">
              {{ verification()!.passed ? 'verified' : 'warning' }}
            </mat-icon>
            <span>
              {{ verification()!.passed ? 'Solution Proof & Verification Passed' : 'Verification Inconclusive' }}
            </span>
          </div>

          @if (verification()!.confidence !== undefined) {
            <span
              [class.bg-emerald-100]="verification()!.passed"
              [class.text-emerald-800]="verification()!.passed"
              [class.dark:bg-emerald-900/60]="verification()!.passed"
              [class.dark:text-emerald-200]="verification()!.passed"
              class="px-2 py-0.5 rounded-full text-[11px] font-bold"
            >
              {{ (verification()!.confidence! * 100).toFixed(0) }}% Confidence
            </span>
          }
        </div>

        @if (verification()!.details) {
          <p class="text-xs text-slate-700 dark:text-slate-300 leading-relaxed">
            {{ verification()!.details }}
          </p>
        }

        @if (verification()!.checks && verification()!.checks!.length > 0) {
          <div class="flex flex-wrap gap-1.5 pt-1">
            @for (chk of verification()!.checks!; track chk) {
              <span class="inline-flex items-center gap-1 text-[11px] font-mono px-2 py-0.5 rounded bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 text-slate-700 dark:text-slate-300">
                <mat-icon class="!w-3 !h-3 !text-[12px] text-emerald-500">check</mat-icon>
                {{ chk }}
              </span>
            }
          </div>
        }
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
export class VerificationPanelComponent {
  verification = input<VerificationResult | undefined>();
}
