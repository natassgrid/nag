import {
  Component,
  ChangeDetectionStrategy,
  input,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { StepJustification } from '../../../../models/explanation-ir.model';
import { MathRendererComponent } from '../math-renderer/math-renderer.component';

@Component({
  selector: 'nag-why-panel',
  standalone: true,
  imports: [CommonModule, MatIconModule, MathRendererComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (hasContent()) {
      <div class="p-4 rounded-xl bg-amber-50/70 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-800/50 shadow-xs flex flex-col gap-2.5">
        <div class="flex items-center gap-2 text-amber-800 dark:text-amber-300 font-semibold text-sm">
          <mat-icon class="!w-4 !h-4 !text-base">lightbulb</mat-icon>
          <span>Why this step?</span>
        </div>

        @if (justification()?.why || justification()?.rationale) {
          <div class="text-xs text-amber-900 dark:text-amber-200 leading-relaxed">
            <nag-explanation-math-renderer
              [content]="justification()?.why || justification()?.rationale || ''"
            ></nag-explanation-math-renderer>
          </div>
        }

        @if (justification()?.rule) {
          <div class="flex items-start gap-1.5 pt-1 border-t border-amber-200/60 dark:border-amber-800/40 text-xs">
            <span class="font-bold text-amber-900 dark:text-amber-300">Rule:</span>
            <span class="text-amber-800 dark:text-amber-200 font-mono text-[11px]">
              {{ justification()?.rule }}
            </span>
          </div>
        }

        @if (justification()?.formula) {
          <div class="flex items-center gap-2 pt-1 border-t border-amber-200/60 dark:border-amber-800/40">
            <span class="text-xs font-bold text-amber-900 dark:text-amber-300">Formula:</span>
            <div class="bg-white/80 dark:bg-slate-900/80 px-2 py-1 rounded-md border border-amber-200/60 dark:border-amber-800/40">
              <nag-explanation-math-renderer
                [content]="justification()?.formula || ''"
                [inline]="true"
              ></nag-explanation-math-renderer>
            </div>
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
export class WhyPanelComponent {
  justification = input<StepJustification | undefined>();

  hasContent(): boolean {
    const j = this.justification();
    return Boolean(j && (j.why || j.rationale || j.rule || j.formula));
  }
}
