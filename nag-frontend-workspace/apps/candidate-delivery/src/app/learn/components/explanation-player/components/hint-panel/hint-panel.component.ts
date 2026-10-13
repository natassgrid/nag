import {
  Component,
  ChangeDetectionStrategy,
  input,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ExplanationHint } from '../../../../models/explanation-ir.model';
import { MathRendererComponent } from '../math-renderer/math-renderer.component';

@Component({
  selector: 'nag-hint-panel',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MathRendererComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (hints() && hints()!.length > 0) {
      <div class="p-4 rounded-xl bg-sky-50/70 dark:bg-sky-950/30 border border-sky-200 dark:border-sky-800/50 shadow-xs flex flex-col gap-3">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-2 text-sky-800 dark:text-sky-300 font-semibold text-sm">
            <mat-icon class="!w-4 !h-4 !text-base">help_outline</mat-icon>
            <span>Progressive Hints ({{ revealedCount() }}/{{ hints()!.length }})</span>
          </div>

          @if (revealedCount() < hints()!.length) {
            <button
              mat-stroked-button
              type="button"
              (click)="onRevealNext()"
              class="!text-xs !py-0 !px-2.5 !h-7 text-sky-700 dark:text-sky-300 border-sky-300 dark:border-sky-700"
            >
              <mat-icon class="!w-3.5 !h-3.5 !text-sm mr-1">visibility</mat-icon>
              Reveal Hint {{ revealedCount() + 1 }}
            </button>
          }
        </div>

        <div class="flex flex-col gap-2">
          @for (hint of hints()!; track hint.id; let idx = $index) {
            @if (idx < revealedCount()) {
              <div class="p-2.5 rounded-lg bg-white/90 dark:bg-slate-900/90 border border-sky-200/60 dark:border-sky-800/40 text-xs flex flex-col gap-1.5 transition-all animate-fadeIn">
                <div class="flex items-center justify-between text-[11px] font-bold uppercase tracking-wider text-sky-600 dark:text-sky-400">
                  <span>Level {{ hint.level || idx + 1 }} Hint</span>
                </div>
                <div class="text-slate-800 dark:text-slate-200 leading-relaxed">
                  <nag-explanation-math-renderer [content]="hint.text"></nag-explanation-math-renderer>
                </div>
                @if (hint.mathExpression) {
                  <div class="mt-1 p-1.5 rounded-md bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700">
                    <nag-explanation-math-renderer [content]="hint.mathExpression" [inline]="true"></nag-explanation-math-renderer>
                  </div>
                }
              </div>
            }
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
      @keyframes fadeIn {
        from {
          opacity: 0;
          transform: translateY(-4px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }
      .animate-fadeIn {
        animation: fadeIn 0.25s ease-out;
      }
    `,
  ],
})
export class HintPanelComponent {
  hints = input<ExplanationHint[] | undefined>();
  readonly revealedCount = signal<number>(1);

  onRevealNext(): void {
    const total = this.hints()?.length ?? 0;
    if (this.revealedCount() < total) {
      this.revealedCount.update((c) => c + 1);
    }
  }
}
